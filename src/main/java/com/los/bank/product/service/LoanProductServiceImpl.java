package com.los.bank.product.service;

import com.los.bank.product.dto.DelegationOfPowerDto;
import com.los.bank.product.dto.LoanProductRequestDto;
import com.los.bank.product.dto.LoanProductResponseDto;
import com.los.bank.product.entity.LoanProduct;
import com.los.bank.product.entity.ProductDelegation;
import com.los.bank.product.exception.LoanProductNotFoundException;
import com.los.bank.product.exception.LoanProductValidationException;
import com.los.bank.product.repository.LoanProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for Loan Product lifecycle operations.
 * Binds to bankTransactionManager for multi-tenant transactional safety.
 */
@Service
public class LoanProductServiceImpl implements LoanProductService {

    private static final Logger logger = LoggerFactory.getLogger(LoanProductServiceImpl.class);

    private static final Set<String> SUPPORTED_EMI_VALUES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("ARREARS", "ADVANCE"))
    );

    private static final Set<String> SUPPORTED_INT_BASE_METHODS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("ACTUAL/365", "ACTUAL/360", "30/360", "ACTUAL/ACTUAL"))
    );

    private final LoanProductRepository loanProductRepository;

    public LoanProductServiceImpl(LoanProductRepository loanProductRepository) {
        this.loanProductRepository = loanProductRepository;
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager")
    public LoanProductResponseDto createLoanProduct(LoanProductRequestDto requestDto) {
        logger.info("Initiating loan product creation: code='{}', name='{}'",
                requestDto != null ? requestDto.getProductCode() : null,
                requestDto != null ? requestDto.getProductName() : null);

        // 1. Comprehensive input validation
        validateLoanProductRequest(requestDto);

        String trimmedCode = requestDto.getProductCode().trim();

        // 2. Uniqueness validation
        if (loanProductRepository.existsByCode(trimmedCode)) {
            logger.warn("Loan product creation failed: product_code '{}' already exists", trimmedCode);
            throw new LoanProductValidationException("Product code already exists.");
        }

        // 3. Map Request DTO to Entity
        LoanProduct loanProduct = LoanProduct.builder()
                .code(trimmedCode)
                .name(requestDto.getProductName().trim())
                .description(requestDto.getDescription() != null ? requestDto.getDescription().trim() : null)
                .status(requestDto.getStatus() != null && !requestDto.getStatus().trim().isEmpty()
                        ? requestDto.getStatus().trim().toUpperCase() : "ACTIVE")
                .emi(requestDto.getEmi().trim())
                .emiType(requestDto.getEmiType().trim())
                .emiFixedDate(normalizeYesNo(requestDto.getEmiFixedDate()))
                .minTenureMonths(requestDto.getMinTenureMonths())
                .maxTenureMonths(requestDto.getMaxTenureMonths())
                .minAmount(requestDto.getMinAmount())
                .maxAmount(requestDto.getMaxAmount())
                .intBaseMethod(requestDto.getIntBaseMethod().trim())
                .noOfDaysInYear(requestDto.getNoOfDaysInYear())
                .penalty(requestDto.getPenalty())
                .penaltyPercent(requestDto.getPenaltyPercent())
                .interestCalculationFrequency(requestDto.getInterestCalculationFrequency().trim())
                .interestCalculationMode(requestDto.getInterestCalculationMode().trim())
                .delegationOfPower(new ArrayList<>())
                .build();

        // 4. Map and attach delegation records
        if (requestDto.getDelegationOfPower() != null) {
            for (DelegationOfPowerDto delDto : requestDto.getDelegationOfPower()) {
                ProductDelegation delegation = ProductDelegation.builder()
                        .sr(delDto.getSr() != null ? delDto.getSr().trim() : null)
                        .limit(delDto.getLimit())
                        .power(delDto.getPower().trim())
                        .loanProduct(loanProduct)
                        .build();
                loanProduct.getDelegationOfPower().add(delegation);
            }
        }

        // 5. Persist parent and child records
        LoanProduct saved = loanProductRepository.save(loanProduct);
        logger.info("Loan product created successfully: ID={}, code='{}'", saved.getId(), saved.getCode());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public Map<String, Object> getAllLoanProducts(int page, int size, String search) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());

        Page<LoanProduct> pagedResult = loanProductRepository.searchProducts(search, pageable);

        List<LoanProductResponseDto> content = pagedResult.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", content);
        response.put("count", content.size());
        response.put("page", pagedResult.getNumber());
        response.put("size", pagedResult.getSize());
        response.put("totalElements", pagedResult.getTotalElements());
        response.put("totalPages", pagedResult.getTotalPages());
        response.put("first", pagedResult.isFirst());
        response.put("last", pagedResult.isLast());
        response.put("hasNext", pagedResult.hasNext());
        response.put("hasPrevious", pagedResult.hasPrevious());

        return response;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public LoanProductResponseDto getLoanProductById(Long id) {
        if (id == null || id <= 0) {
            throw new LoanProductValidationException("Valid loan product ID is required.");
        }

        LoanProduct product = loanProductRepository.findByIdWithDelegations(id)
                .orElseThrow(() -> new LoanProductNotFoundException("Loan product not found with ID: " + id));

        return mapToResponseDto(product);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public LoanProductResponseDto getLoanProductByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new LoanProductValidationException("Product code is required.");
        }

        String trimmedCode = code.trim();
        LoanProduct product = loanProductRepository.findByCodeWithDelegations(trimmedCode)
                .orElseThrow(() -> new LoanProductNotFoundException("Loan product not found with code: " + trimmedCode));

        return mapToResponseDto(product);
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager")
    public LoanProductResponseDto updateLoanProduct(Long id, LoanProductRequestDto requestDto) {
        if (id == null || id <= 0) {
            throw new LoanProductValidationException("Valid loan product ID is required.");
        }

        LoanProduct existing = loanProductRepository.findByIdWithDelegations(id)
                .orElseThrow(() -> new LoanProductNotFoundException("Loan product not found with ID: " + id));

        // 1. Comprehensive input validation
        validateLoanProductRequest(requestDto);

        String trimmedCode = requestDto.getProductCode().trim();

        // 2. Uniqueness check excluding current product
        if (loanProductRepository.existsByCodeAndIdNot(trimmedCode, id)) {
            logger.warn("Loan product update failed: product_code '{}' already exists on another product", trimmedCode);
            throw new LoanProductValidationException("Product code already exists.");
        }

        // 3. Update main entity fields
        existing.setCode(trimmedCode);
        existing.setName(requestDto.getProductName().trim());
        if (requestDto.getDescription() != null) {
            existing.setDescription(requestDto.getDescription().trim());
        }
        if (requestDto.getStatus() != null && !requestDto.getStatus().trim().isEmpty()) {
            existing.setStatus(requestDto.getStatus().trim().toUpperCase());
        }
        existing.setEmi(requestDto.getEmi().trim());
        existing.setEmiType(requestDto.getEmiType().trim());
        if (requestDto.getEmiFixedDate() != null && !requestDto.getEmiFixedDate().trim().isEmpty()) {
            existing.setEmiFixedDate(normalizeYesNo(requestDto.getEmiFixedDate()));
        }
        existing.setMinTenureMonths(requestDto.getMinTenureMonths());
        existing.setMaxTenureMonths(requestDto.getMaxTenureMonths());
        existing.setMinAmount(requestDto.getMinAmount());
        existing.setMaxAmount(requestDto.getMaxAmount());
        existing.setIntBaseMethod(requestDto.getIntBaseMethod().trim());
        existing.setNoOfDaysInYear(requestDto.getNoOfDaysInYear());
        existing.setPenalty(requestDto.getPenalty());
        existing.setPenaltyPercent(requestDto.getPenaltyPercent());
        existing.setInterestCalculationFrequency(requestDto.getInterestCalculationFrequency().trim());
        existing.setInterestCalculationMode(requestDto.getInterestCalculationMode().trim());

        // 4. Synchronize delegation records without duplicate children
        if (requestDto.getDelegationOfPower() != null) {
            List<ProductDelegation> updatedDelegations = new ArrayList<>();
            for (DelegationOfPowerDto delDto : requestDto.getDelegationOfPower()) {
                ProductDelegation delegation = ProductDelegation.builder()
                        .sr(delDto.getSr() != null ? delDto.getSr().trim() : null)
                        .limit(delDto.getLimit())
                        .power(delDto.getPower().trim())
                        .loanProduct(existing)
                        .build();
                updatedDelegations.add(delegation);
            }
            existing.setDelegationOfPower(updatedDelegations);
        }

        LoanProduct updated = loanProductRepository.save(existing);
        logger.info("Loan product updated successfully: ID={}, code='{}'", updated.getId(), updated.getCode());

        return mapToResponseDto(updated);
    }

    /**
     * Validates all required fields, ranges, and supported values against LOS business rules.
     */
    private void validateLoanProductRequest(LoanProductRequestDto dto) {
        if (dto == null) {
            throw new LoanProductValidationException("Request payload cannot be null.");
        }

        // 1. product_code
        if (dto.getProductCode() == null || dto.getProductCode().trim().isEmpty()) {
            throw new LoanProductValidationException("Product code is required.");
        }

        // 2. product_name
        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty()) {
            throw new LoanProductValidationException("Product name is required.");
        }

        // 3. emi (e.g. Arrears, Advance)
        if (dto.getEmi() == null || dto.getEmi().trim().isEmpty() ||
                !SUPPORTED_EMI_VALUES.contains(dto.getEmi().trim().toUpperCase())) {
            throw new LoanProductValidationException("Please select a valid EMI type.");
        }

        // 4. emi_type (e.g. Int in Advance, Int in Arrears, Regular, Reducing, etc.)
        if (dto.getEmiType() == null || dto.getEmiType().trim().isEmpty()) {
            throw new LoanProductValidationException("Please select a valid EMI calculation type.");
        }

        // 4a. emi_fixed_date (optional Yes/No)
        if (dto.getEmiFixedDate() != null && !dto.getEmiFixedDate().trim().isEmpty()) {
            String fixedDate = dto.getEmiFixedDate().trim();
            if (!fixedDate.equalsIgnoreCase("Yes") && !fixedDate.equalsIgnoreCase("No")
                    && !fixedDate.equalsIgnoreCase("true") && !fixedDate.equalsIgnoreCase("false")) {
                throw new LoanProductValidationException("EMI fixed date must be either 'Yes' or 'No'.");
            }
        }

        // 5. min_tenure_months
        if (dto.getMinTenureMonths() == null || dto.getMinTenureMonths() <= 0) {
            throw new LoanProductValidationException("Minimum tenure must be greater than zero.");
        }

        // 6. max_tenure_months
        if (dto.getMaxTenureMonths() == null || dto.getMaxTenureMonths() < dto.getMinTenureMonths()) {
            throw new LoanProductValidationException("Maximum tenure must be greater than or equal to minimum tenure.");
        }

        // 7. min_amount
        if (dto.getMinAmount() == null || dto.getMinAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new LoanProductValidationException("Minimum loan amount must be greater than zero.");
        }

        // 8. max_amount
        if (dto.getMaxAmount() == null || dto.getMaxAmount().compareTo(dto.getMinAmount()) < 0) {
            throw new LoanProductValidationException("Maximum loan amount must be greater than or equal to minimum amount.");
        }

        // 9. int_base_method (e.g. Actual/365, Actual/360, 30/360)
        if (dto.getIntBaseMethod() == null || dto.getIntBaseMethod().trim().isEmpty() ||
                !SUPPORTED_INT_BASE_METHODS.contains(dto.getIntBaseMethod().trim().toUpperCase())) {
            throw new LoanProductValidationException("Please select a valid interest base method.");
        }

        // 10. no_of_days_in_year
        if (dto.getNoOfDaysInYear() == null || dto.getNoOfDaysInYear() <= 0) {
            throw new LoanProductValidationException("Number of days in a year must be greater than zero.");
        }

        // 11. interest_calculation_frequency
        if (dto.getInterestCalculationFrequency() == null || dto.getInterestCalculationFrequency().trim().isEmpty()) {
            throw new LoanProductValidationException("Interest calculation frequency is required.");
        }

        // 12. interest_calculation_mode
        if (dto.getInterestCalculationMode() == null || dto.getInterestCalculationMode().trim().isEmpty()) {
            throw new LoanProductValidationException("Interest calculation mode is required.");
        }

        // 13. penalty
        if (dto.getPenalty() == null) {
            throw new LoanProductValidationException("Invalid penalty configuration.");
        }

        // 14. penalty_percent
        if (dto.getPenaltyPercent() == null || dto.getPenaltyPercent().compareTo(BigDecimal.ZERO) < 0) {
            throw new LoanProductValidationException("Please enter a valid penalty percentage.");
        }

        // 15. delegation_of_power
        if (dto.getDelegationOfPower() != null) {
            for (DelegationOfPowerDto delegation : dto.getDelegationOfPower()) {
                if (delegation == null) {
                    throw new LoanProductValidationException("Please provide valid delegation of power details.");
                }
                if (delegation.getSr() == null || delegation.getSr().trim().isEmpty()) {
                    throw new LoanProductValidationException("Delegation serial number is required.");
                }
                if (delegation.getLimit() == null || delegation.getLimit().compareTo(BigDecimal.ZERO) < 0) {
                    throw new LoanProductValidationException("Please enter a valid delegation limit.");
                }
                if (delegation.getPower() == null || delegation.getPower().trim().isEmpty()) {
                    throw new LoanProductValidationException("Delegation power is required.");
                }
            }
        }
    }

    private LoanProductResponseDto mapToResponseDto(LoanProduct entity) {
        if (entity == null) {
            return null;
        }

        List<DelegationOfPowerDto> delegationDtos = new ArrayList<>();
        if (entity.getDelegationOfPower() != null) {
            for (ProductDelegation del : entity.getDelegationOfPower()) {
                delegationDtos.add(DelegationOfPowerDto.builder()
                        .id(del.getId())
                        .sr(del.getSr())
                        .limit(del.getLimit())
                        .power(del.getPower())
                        .build());
            }
        }

        return LoanProductResponseDto.builder()
                .id(entity.getId())
                .productCode(entity.getCode())
                .productName(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .emi(entity.getEmi())
                .emiType(entity.getEmiType())
                .emiFixedDate(entity.getEmiFixedDate() != null ? entity.getEmiFixedDate() : "No")
                .minTenureMonths(entity.getMinTenureMonths())
                .maxTenureMonths(entity.getMaxTenureMonths())
                .minAmount(entity.getMinAmount())
                .maxAmount(entity.getMaxAmount())
                .intBaseMethod(entity.getIntBaseMethod())
                .noOfDaysInYear(entity.getNoOfDaysInYear())
                .interestCalculationFrequency(entity.getInterestCalculationFrequency())
                .interestCalculationMode(entity.getInterestCalculationMode())
                .penalty(entity.getPenalty())
                .penaltyPercent(entity.getPenaltyPercent())
                .delegationOfPower(delegationDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String normalizeYesNo(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "No";
        }
        String trimmed = value.trim();
        return (trimmed.equalsIgnoreCase("Yes") || trimmed.equalsIgnoreCase("true")) ? "Yes" : "No";
    }
}
