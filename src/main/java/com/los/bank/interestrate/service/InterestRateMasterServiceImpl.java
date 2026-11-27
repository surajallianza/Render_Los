package com.los.bank.interestrate.service;

import com.los.bank.interestrate.dto.*;
import com.los.bank.interestrate.entity.InterestRateMaster;
import com.los.bank.interestrate.entity.InterestRateStatus;
import com.los.bank.interestrate.entity.InterestRateTier;
import com.los.bank.interestrate.exception.InterestRateConflictException;
import com.los.bank.interestrate.exception.InterestRateNotFoundException;
import com.los.bank.interestrate.exception.InterestRateValidationException;
import com.los.bank.interestrate.repository.InterestRateMasterRepository;
import com.los.bank.product.entity.LoanProduct;
import com.los.bank.product.repository.LoanProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interest Rate Master: effective-dated, tiered interest rates per loan product,
 * with maker-checker approval.
 *
 * <p>Tier rule: a loan of amount A and tenure T uses the smallest tier that covers
 * both, i.e. among tiers with upToAmount >= A and upToMonths >= T, the one with
 * the lowest upToAmount, then the lowest upToMonths.
 */
@Service
@Transactional(transactionManager = "bankTransactionManager")
public class InterestRateMasterServiceImpl implements InterestRateMasterService {

    private static final Logger logger = LoggerFactory.getLogger(InterestRateMasterServiceImpl.class);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_MONTHS = 480;
    private static final int MAX_TIERS = 50;
    private static final int MAX_TEXT = 255;

    private final InterestRateMasterRepository rateMasterRepository;
    private final LoanProductRepository loanProductRepository;

    public InterestRateMasterServiceImpl(InterestRateMasterRepository rateMasterRepository,
                                         LoanProductRepository loanProductRepository) {
        this.rateMasterRepository = rateMasterRepository;
        this.loanProductRepository = loanProductRepository;
    }

    // ---------------------------------------------------------------------
    // Maker actions
    // ---------------------------------------------------------------------

    @Override
    public InterestRateMasterResponse create(InterestRateMasterRequest request, Long staffUserId) {
        LoanProduct product = validateRequest(request, null);

        InterestRateMaster master = InterestRateMaster.builder()
                .productId(product.getId())
                .effectiveDate(request.getEffectiveDate())
                .status(InterestRateStatus.PENDING)
                .remarks(trimToNull(request.getRemarks()))
                .createdBy(staffUserId)
                .createdAt(LocalDateTime.now())
                .build();
        master.replaceTiers(toTiers(request.getTiers()));
        master = rateMasterRepository.save(master);

        logger.info("Interest rate sheet {} created for product {} effective {} by user {}",
                master.getId(), product.getCode(), master.getEffectiveDate(), staffUserId);
        return toResponse(master, product, true);
    }

    @Override
    public InterestRateMasterResponse update(Long id, InterestRateMasterRequest request, Long staffUserId) {
        InterestRateMaster master = findMaster(id);
        requireEditable(master, "edited");
        LoanProduct product = validateRequest(request, id);

        // Hibernate inserts before it deletes orphans, so remove the old tiers first;
        // otherwise the new rows clash with the unique (sr_no) / (amount, months) keys.
        master.getTiers().clear();
        rateMasterRepository.saveAndFlush(master);

        master.setProductId(product.getId());
        master.setEffectiveDate(request.getEffectiveDate());
        master.setRemarks(trimToNull(request.getRemarks()));
        master.replaceTiers(toTiers(request.getTiers()));
        // Any change needs a fresh approval
        master.setStatus(InterestRateStatus.PENDING);
        master.setRejectionReason(null);
        master.setReviewedBy(null);
        master.setReviewedAt(null);
        master.setModifiedBy(staffUserId);
        master.setUpdatedAt(LocalDateTime.now());
        master = rateMasterRepository.save(master);

        logger.info("Interest rate sheet {} updated by user {}; status reset to PENDING", id, staffUserId);
        return toResponse(master, product, true);
    }

    @Override
    public void delete(Long id, Long staffUserId) {
        InterestRateMaster master = findMaster(id);
        requireEditable(master, "deleted");
        rateMasterRepository.delete(master);
        logger.info("Interest rate sheet {} deleted by user {}", id, staffUserId);
    }

    // ---------------------------------------------------------------------
    // Checker actions
    // ---------------------------------------------------------------------

    @Override
    public InterestRateMasterResponse approve(Long id, Long staffUserId) {
        InterestRateMaster master = findMaster(id);
        requirePending(master);
        requireDifferentChecker(master, staffUserId);
        if (master.getEffectiveDate().isBefore(today())) {
            throw new InterestRateValidationException("effectiveDate",
                    "Effective date " + master.getEffectiveDate() + " has already passed. Edit the sheet with a current or future date before approving.");
        }
        if (rateMasterRepository.existsLiveSheet(master.getProductId(), master.getEffectiveDate(), master.getId())) {
            throw new InterestRateConflictException("Another rate sheet already exists for this product and effective date");
        }

        master.setStatus(InterestRateStatus.APPROVED);
        master.setRejectionReason(null);
        master.setReviewedBy(staffUserId);
        master.setReviewedAt(LocalDateTime.now());
        master = rateMasterRepository.save(master);

        logger.info("Interest rate sheet {} approved by user {}", id, staffUserId);
        return toResponse(master, findProductById(master.getProductId()), false);
    }

    @Override
    public InterestRateMasterResponse reject(Long id, String reason, Long staffUserId) {
        InterestRateMaster master = findMaster(id);
        requirePending(master);
        requireDifferentChecker(master, staffUserId);
        String cleanReason = trimToNull(reason);
        if (cleanReason == null) {
            throw new InterestRateValidationException("reason", "Rejection reason is required");
        }
        if (cleanReason.length() > MAX_TEXT) {
            throw new InterestRateValidationException("reason", "Rejection reason must be at most " + MAX_TEXT + " characters");
        }

        master.setStatus(InterestRateStatus.REJECTED);
        master.setRejectionReason(cleanReason);
        master.setReviewedBy(staffUserId);
        master.setReviewedAt(LocalDateTime.now());
        master = rateMasterRepository.save(master);

        logger.info("Interest rate sheet {} rejected by user {}", id, staffUserId);
        return toResponse(master, findProductById(master.getProductId()), false);
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public InterestRateMasterResponse getById(Long id) {
        InterestRateMaster master = findMaster(id);
        return toResponse(master, findProductById(master.getProductId()), true);
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public InterestRatePageResponse<InterestRateMasterResponse> list(String productCode, String status, int page, int size) {
        Long productId = null;
        if (!isBlank(productCode)) {
            productId = findProductByCode(productCode).getId();
        }
        String statusFilter = null;
        if (!isBlank(status)) {
            statusFilter = status.trim().toUpperCase(Locale.ROOT);
            if (!Set.of(InterestRateStatus.PENDING, InterestRateStatus.APPROVED, InterestRateStatus.REJECTED).contains(statusFilter)) {
                throw new InterestRateValidationException("status", "Status must be PENDING, APPROVED or REJECTED");
            }
        }

        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Order.asc("productId"), Sort.Order.desc("effectiveDate"), Sort.Order.desc("id")));
        Page<InterestRateMaster> result = rateMasterRepository.search(productId, statusFilter, pageable);

        Set<Long> productIds = result.getContent().stream().map(InterestRateMaster::getProductId).collect(Collectors.toSet());
        Map<Long, LoanProduct> products = loanProductRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(LoanProduct::getId, Function.identity()));

        List<InterestRateMasterResponse> content = result.getContent().stream()
                .map(m -> toResponse(m, products.get(m.getProductId()), false))
                .toList();

        return InterestRatePageResponse.<InterestRateMasterResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public ApplicableRateResponse findApplicableRate(String productCode, BigDecimal amount, Integer tenureMonths, LocalDate asOfDate) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(productCode)) {
            errors.put("productCode", "Product code is required");
        }
        if (amount == null || amount.signum() <= 0) {
            errors.put("amount", "Loan amount must be greater than zero");
        }
        if (tenureMonths == null || tenureMonths < 1 || tenureMonths > MAX_MONTHS) {
            errors.put("tenureMonths", "Tenure must be between 1 and " + MAX_MONTHS + " months");
        }
        if (!errors.isEmpty()) {
            throw new InterestRateValidationException("Invalid rate lookup request", errors);
        }

        LoanProduct product = findProductByCode(productCode);
        if (product.getMinAmount() != null && amount.compareTo(product.getMinAmount()) < 0) {
            errors.put("amount", "Loan amount is below the product minimum of " + product.getMinAmount().toPlainString());
        }
        if (product.getMaxAmount() != null && amount.compareTo(product.getMaxAmount()) > 0) {
            errors.put("amount", "Loan amount is above the product maximum of " + product.getMaxAmount().toPlainString());
        }
        if (product.getMinTenureMonths() != null && tenureMonths < product.getMinTenureMonths()) {
            errors.put("tenureMonths", "Tenure is below the product minimum of " + product.getMinTenureMonths() + " months");
        }
        if (product.getMaxTenureMonths() != null && tenureMonths > product.getMaxTenureMonths()) {
            errors.put("tenureMonths", "Tenure is above the product maximum of " + product.getMaxTenureMonths() + " months");
        }
        if (!errors.isEmpty()) {
            throw new InterestRateValidationException("Loan is outside the product limits", errors);
        }

        LocalDate date = asOfDate != null ? asOfDate : today();
        InterestRateMaster master = rateMasterRepository
                .findFirstByProductIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        product.getId(), InterestRateStatus.APPROVED, date)
                .orElseThrow(() -> new InterestRateNotFoundException(
                        "No approved interest rate sheet for product " + product.getCode() + " on " + date));

        InterestRateTier tier = selectTier(master.getTiers(), amount, tenureMonths)
                .orElseThrow(() -> new InterestRateNotFoundException(
                        "The rate sheet effective " + master.getEffectiveDate() + " for product " + product.getCode()
                                + " has no tier for amount " + amount.toPlainString() + " and tenure " + tenureMonths + " months"));

        BigDecimal penalRate = tier.getPenalRate();
        String penalSource = penalRate != null ? "TIER" : null;
        if (penalRate == null && Boolean.TRUE.equals(product.getPenalty()) && product.getPenaltyPercent() != null) {
            penalRate = product.getPenaltyPercent();
            penalSource = "PRODUCT";
        }

        return ApplicableRateResponse.builder()
                .productCode(product.getCode())
                .productName(product.getName())
                .amount(amount)
                .tenureMonths(tenureMonths)
                .asOfDate(date)
                .rateMasterId(master.getId())
                .effectiveDate(master.getEffectiveDate())
                .tierId(tier.getId())
                .tierSrNo(tier.getSrNo())
                .upToAmount(tier.getUpToAmount())
                .upToMonths(tier.getUpToMonths())
                .normalRate(tier.getNormalRate())
                .penalRate(penalRate)
                .penalRateSource(penalSource)
                .adhocRate(tier.getAdhocRate())
                .build();
    }

    /**
     * Smallest tier covering both the amount and the tenure.
     */
    static Optional<InterestRateTier> selectTier(List<InterestRateTier> tiers, BigDecimal amount, int tenureMonths) {
        return tiers.stream()
                .filter(t -> t.getUpToAmount().compareTo(amount) >= 0 && t.getUpToMonths() >= tenureMonths)
                .min(Comparator.comparing(InterestRateTier::getUpToAmount)
                        .thenComparing(InterestRateTier::getUpToMonths));
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    private LoanProduct validateRequest(InterestRateMasterRequest request, Long excludeId) {
        if (request == null) {
            throw new InterestRateValidationException("request", "Request body is required");
        }
        Map<String, String> errors = new LinkedHashMap<>();

        LoanProduct product = null;
        if (isBlank(request.getProductCode())) {
            errors.put("productCode", "Product code is required");
        } else {
            product = loanProductRepository.findByCodeIgnoreCase(request.getProductCode().trim()).orElse(null);
            if (product == null) {
                errors.put("productCode", "Loan product not found: " + request.getProductCode().trim());
            } else if ("INACTIVE".equalsIgnoreCase(product.getStatus())) {
                errors.put("productCode", "Loan product " + product.getCode() + " is inactive");
            }
        }

        if (request.getEffectiveDate() == null) {
            errors.put("effectiveDate", "Effective date is required");
        } else if (request.getEffectiveDate().isBefore(today())) {
            errors.put("effectiveDate", "Effective date cannot be in the past");
        }

        if (request.getRemarks() != null && request.getRemarks().trim().length() > MAX_TEXT) {
            errors.put("remarks", "Remarks must be at most " + MAX_TEXT + " characters");
        }

        List<InterestRateTierRequest> tiers = request.getTiers();
        if (tiers == null || tiers.isEmpty()) {
            errors.put("tiers", "Add at least one rate tier");
        } else if (tiers.size() > MAX_TIERS) {
            errors.put("tiers", "A rate sheet can have at most " + MAX_TIERS + " tiers");
        } else {
            Set<String> seenBands = new HashSet<>();
            for (int i = 0; i < tiers.size(); i++) {
                validateTier(tiers.get(i), i, product, seenBands, errors);
            }
        }

        if (!errors.isEmpty()) {
            throw new InterestRateValidationException("Interest rate sheet validation failed", errors);
        }

        if (rateMasterRepository.existsLiveSheet(product.getId(), request.getEffectiveDate(), excludeId)) {
            throw new InterestRateConflictException("A rate sheet already exists for product " + product.getCode()
                    + " effective " + request.getEffectiveDate() + ". Choose a different effective date.");
        }
        return product;
    }

    private void validateTier(InterestRateTierRequest tier, int index, LoanProduct product,
                              Set<String> seenBands, Map<String, String> errors) {
        String prefix = "tiers[" + index + "].";
        if (tier == null) {
            errors.put("tiers[" + index + "]", "Tier is empty");
            return;
        }

        BigDecimal amount = tier.getUpToAmount();
        if (amount == null || amount.signum() <= 0) {
            errors.put(prefix + "upToAmount", "Up to amount must be greater than zero");
        } else if (amount.stripTrailingZeros().scale() > 2) {
            errors.put(prefix + "upToAmount", "Up to amount can have at most 2 decimal places");
        } else if (product != null && product.getMinAmount() != null && amount.compareTo(product.getMinAmount()) < 0) {
            errors.put(prefix + "upToAmount", "Up to amount is below the product minimum amount of "
                    + product.getMinAmount().toPlainString() + ", so no loan could use this tier");
        }

        Integer months = tier.getUpToMonths();
        if (months == null || months < 1 || months > MAX_MONTHS) {
            errors.put(prefix + "upToMonths", "Up to months must be between 1 and " + MAX_MONTHS);
        } else if (product != null && product.getMinTenureMonths() != null && months < product.getMinTenureMonths()) {
            errors.put(prefix + "upToMonths", "Up to months is below the product minimum tenure of "
                    + product.getMinTenureMonths() + " months, so no loan could use this tier");
        }

        validateRate(tier.getNormalRate(), prefix + "normalRate", "Normal interest rate", true, errors);
        validateRate(tier.getPenalRate(), prefix + "penalRate", "Penal rate", false, errors);
        validateRate(tier.getAdhocRate(), prefix + "adhocRate", "Adhoc rate", false, errors);

        if (amount != null && months != null && !seenBands.add(amount.stripTrailingZeros().toPlainString() + "|" + months)) {
            errors.put(prefix + "upToAmount", "Another tier already has up to amount " + amount.toPlainString()
                    + " and up to months " + months);
        }
    }

    private void validateRate(BigDecimal rate, String key, String label, boolean required, Map<String, String> errors) {
        if (rate == null) {
            if (required) {
                errors.put(key, label + " is required");
            }
            return;
        }
        if (rate.signum() < 0 || rate.compareTo(HUNDRED) > 0) {
            errors.put(key, label + " must be between 0 and 100");
        } else if (rate.stripTrailingZeros().scale() > 2) {
            errors.put(key, label + " can have at most 2 decimal places");
        }
    }

    private void requireEditable(InterestRateMaster master, String action) {
        if (InterestRateStatus.APPROVED.equals(master.getStatus()) && !master.getEffectiveDate().isAfter(today())) {
            throw new InterestRateConflictException("This rate sheet is approved and in effect since "
                    + master.getEffectiveDate() + " and cannot be " + action
                    + ". Create a new rate sheet with a later effective date instead.");
        }
    }

    private void requirePending(InterestRateMaster master) {
        if (!InterestRateStatus.PENDING.equals(master.getStatus())) {
            throw new InterestRateConflictException("Only PENDING rate sheets can be approved or rejected (current status: "
                    + master.getStatus() + ")");
        }
    }

    private void requireDifferentChecker(InterestRateMaster master, Long staffUserId) {
        if (staffUserId == null) {
            throw new InterestRateConflictException("The approving user could not be identified");
        }
        Long maker = master.getModifiedBy() != null ? master.getModifiedBy() : master.getCreatedBy();
        if (staffUserId.equals(maker)) {
            throw new InterestRateConflictException("The user who created or last edited a rate sheet cannot approve or reject it");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private List<InterestRateTier> toTiers(List<InterestRateTierRequest> requests) {
        List<InterestRateTierRequest> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparing(InterestRateTierRequest::getUpToAmount)
                .thenComparing(InterestRateTierRequest::getUpToMonths));
        List<InterestRateTier> tiers = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            InterestRateTierRequest r = sorted.get(i);
            tiers.add(InterestRateTier.builder()
                    .srNo(i + 1)
                    .upToAmount(r.getUpToAmount())
                    .upToMonths(r.getUpToMonths())
                    .normalRate(r.getNormalRate())
                    .penalRate(r.getPenalRate())
                    .adhocRate(r.getAdhocRate())
                    .build());
        }
        return tiers;
    }

    private InterestRateMasterResponse toResponse(InterestRateMaster master, LoanProduct product, boolean withWarnings) {
        LocalDate today = today();
        boolean currentlyApplied = InterestRateStatus.APPROVED.equals(master.getStatus())
                && !master.getEffectiveDate().isAfter(today)
                && rateMasterRepository.findFirstByProductIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        master.getProductId(), InterestRateStatus.APPROVED, today)
                .map(current -> current.getId().equals(master.getId()))
                .orElse(false);
        boolean editable = !(InterestRateStatus.APPROVED.equals(master.getStatus()) && !master.getEffectiveDate().isAfter(today));

        List<InterestRateTierResponse> tiers = master.getTiers().stream()
                .sorted(Comparator.comparing(InterestRateTier::getSrNo))
                .map(t -> InterestRateTierResponse.builder()
                        .id(t.getId())
                        .srNo(t.getSrNo())
                        .upToAmount(t.getUpToAmount())
                        .upToMonths(t.getUpToMonths())
                        .normalRate(t.getNormalRate())
                        .penalRate(t.getPenalRate())
                        .adhocRate(t.getAdhocRate())
                        .build())
                .toList();

        return InterestRateMasterResponse.builder()
                .id(master.getId())
                .productId(master.getProductId())
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .effectiveDate(master.getEffectiveDate())
                .status(master.getStatus())
                .currentlyApplied(currentlyApplied)
                .editable(editable)
                .remarks(master.getRemarks())
                .rejectionReason(master.getRejectionReason())
                .createdBy(master.getCreatedBy())
                .createdAt(master.getCreatedAt())
                .modifiedBy(master.getModifiedBy())
                .updatedAt(master.getUpdatedAt())
                .reviewedBy(master.getReviewedBy())
                .reviewedAt(master.getReviewedAt())
                .tiers(tiers)
                .warnings(withWarnings ? coverageWarnings(master.getTiers(), product) : null)
                .build();
    }

    /**
     * Loans the product allows but no tier covers (they would get "no rate" at application time).
     */
    private List<String> coverageWarnings(List<InterestRateTier> tiers, LoanProduct product) {
        List<String> warnings = new ArrayList<>();
        if (product == null || tiers.isEmpty()) {
            return warnings;
        }
        BigDecimal maxAmount = product.getMaxAmount();
        Integer maxMonths = product.getMaxTenureMonths();
        if (maxAmount != null && maxMonths != null && selectTier(tiers, maxAmount, maxMonths).isEmpty()) {
            BigDecimal highestAmount = tiers.stream().map(InterestRateTier::getUpToAmount).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            int highestMonths = tiers.stream().mapToInt(InterestRateTier::getUpToMonths).max().orElse(0);
            warnings.add("No tier covers the product maximum of " + maxAmount.toPlainString() + " for " + maxMonths
                    + " months (highest tier: up to " + highestAmount.toPlainString() + " / " + highestMonths
                    + " months). Such loans will get no rate.");
        }
        return warnings;
    }

    private InterestRateMaster findMaster(Long id) {
        return rateMasterRepository.findById(id)
                .orElseThrow(() -> new InterestRateNotFoundException("Interest rate sheet not found with ID: " + id));
    }

    private LoanProduct findProductByCode(String productCode) {
        return loanProductRepository.findByCodeIgnoreCase(productCode.trim())
                .orElseThrow(() -> new InterestRateNotFoundException("Loan product not found: " + productCode.trim()));
    }

    private LoanProduct findProductById(Long productId) {
        return loanProductRepository.findById(productId).orElse(null);
    }

    LocalDate today() {
        return LocalDate.now();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
