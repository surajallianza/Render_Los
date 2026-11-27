package com.los.bank.loanEligibility.service;

import com.los.bank.loanEligibility.dto.EligibilityRequest;
import com.los.bank.loanEligibility.dto.EligibilityResponse;
import com.los.bank.loanEligibility.entity.EligibilityAssessment;
import com.los.bank.loanEligibility.entity.EligibilityRuleResult;
import com.los.bank.loanEligibility.exception.EligibilityNotFoundException;
import com.los.bank.loanEligibility.repository.EligibilityAssessmentRepository;
import com.los.bank.rule.entity.RuleMetric;
import com.los.bank.rule.service.ResolvedRule;
import com.los.bank.rule.service.RuleEvaluator;
import com.los.bank.rule.service.RuleEvaluator.Outcome;
import com.los.bank.rule.service.RuleEvaluator.RuleCheck;
import com.los.bank.rule.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Loan eligibility: FOIR / DTI / LTV are calculated here, the CIBIL score is taken as entered,
 * and everything is checked against the Rule Engine's active rules.
 *
 * <ul>
 *   <li>No rule rejects and none refers: <b>Eligible</b> for the requested amount</li>
 *   <li>A Refer rule matched, or only FOIR / DTI / LTV rules rejected and a lower amount passes:
 *       <b>Refer for Review</b>, with the eligible (lower) amount</li>
 *   <li>Otherwise <b>Not Eligible</b>, eligible amount 0</li>
 * </ul>
 * Override rules add their rate offset to the interest rate.
 */
@Service
@Transactional(transactionManager = "bankTransactionManager")
public class LoanEligibilityServiceImpl implements LoanEligibilityService {

    private static final Logger logger = LoggerFactory.getLogger(LoanEligibilityServiceImpl.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    public static final String ELIGIBLE = "ELIGIBLE";
    public static final String REFER_FOR_REVIEW = "REFER_FOR_REVIEW";
    public static final String NOT_ELIGIBLE = "NOT_ELIGIBLE";

    private static final Map<String, String> DISPLAY = Map.of(
            ELIGIBLE, "Eligible", REFER_FOR_REVIEW, "Refer for Review", NOT_ELIGIBLE, "Not Eligible");

    private final RuleService ruleService;
    private final RuleEvaluator ruleEvaluator;
    private final EligibilityCalculator calculator;
    private final EligibilityAssessmentRepository assessmentRepository;

    public LoanEligibilityServiceImpl(RuleService ruleService,
                                      RuleEvaluator ruleEvaluator,
                                      EligibilityCalculator calculator,
                                      EligibilityAssessmentRepository assessmentRepository) {
        this.ruleService = ruleService;
        this.ruleEvaluator = ruleEvaluator;
        this.calculator = calculator;
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    public EligibilityResponse assess(EligibilityRequest request, Long staffUserId) {
        EligibilityCalculator.Inputs in = calculator.prepare(request);
        List<String> warnings = new ArrayList<>(in.warnings());
        Map<String, String> facts = normalizedFacts(request.getAdditionalFacts());

        List<ResolvedRule> rules = ruleService.getRulesInForce(LocalDate.now());
        if (rules.isEmpty()) {
            warnings.add("No active rules are set up in the Rule Engine, so every check passes.");
        }
        rules.stream().filter(r -> !r.isUsable()).forEach(r -> warnings.add("Rule #" + r.rule().getId()
                + " was skipped: its operator or decision lookup is not understood."));

        BigDecimal requested = in.requestedAmount();
        List<RuleCheck> checks = ruleEvaluator.evaluate(rules, valuesFor(in, rules, requested, facts, warnings));
        addNotCheckedWarnings(checks, in, warnings);

        List<String> reasons = new ArrayList<>();
        List<RuleCheck> rejected = checks.stream().filter(c -> c.outcome() == Outcome.REJECTED).toList();
        String decision;
        BigDecimal eligibleAmount;
        List<RuleCheck> finalChecks = checks;

        if (rejected.isEmpty()) {
            eligibleAmount = requested;
            decision = RuleEvaluator.hasOutcome(checks, Outcome.REFERRED) ? REFER_FOR_REVIEW : ELIGIBLE;
        } else {
            rejected.forEach(c -> reasons.add(c.message()));
            boolean amountCanFix = rejected.stream().allMatch(c -> c.rule().param().metric() != null
                    && c.rule().param().metric().isDependsOnLoanAmount());
            eligibleAmount = amountCanFix ? maxPassingAmount(in, rules, facts) : BigDecimal.ZERO;
            if (eligibleAmount.signum() > 0) {
                decision = REFER_FOR_REVIEW;
                finalChecks = ruleEvaluator.evaluate(rules, valuesFor(in, rules, eligibleAmount, facts, null));
                reasons.add("The requested amount of Rs. " + money(requested) + " is higher than the indicative "
                        + "eligible amount of Rs. " + money(eligibleAmount) + ".");
            } else {
                decision = NOT_ELIGIBLE;
            }
        }
        finalChecks.stream().filter(c -> c.outcome() == Outcome.REFERRED || c.outcome() == Outcome.OVERRIDDEN)
                .map(RuleCheck::message).filter(m -> !reasons.contains(m)).forEach(reasons::add);

        eligibleAmount = eligibleAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rateOffset = RuleEvaluator.totalRateOffset(finalChecks).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalRate = in.annualInterestRate().add(rateOffset).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proposedEmi = EligibilityCalculator.emi(requested, in.annualInterestRate(), in.tenureMonths());
        BigDecimal eligibleEmi = EligibilityCalculator.emi(eligibleAmount, finalRate, in.tenureMonths());
        Map<RuleMetric, BigDecimal> metrics = calculator.metrics(in, requested);

        EligibilityAssessment assessment = EligibilityAssessment.builder()
                .leadId(trimToNull(request.getLeadId()))
                .applicantName(trimToNull(request.getApplicantName()))
                .productName(trimToNull(request.getProductName()))
                .requestedAmount(requested)
                .annualInterestRate(in.annualInterestRate())
                .tenureMonths(in.tenureMonths())
                .securityValue(in.securityValue())
                .existingMonthlyDebt(in.existingMonthlyDebt())
                .totalIncome(in.totalIncome())
                .totalExpenses(in.totalExpenses())
                .netIncome(in.netIncome())
                .cibilScore(in.cibilScore())
                .proposedEmi(proposedEmi)
                .foir(metrics.get(RuleMetric.FOIR))
                .dti(metrics.get(RuleMetric.DTI))
                .ltv(metrics.get(RuleMetric.LTV))
                .decision(decision)
                .eligibleAmount(eligibleAmount)
                .rateOffset(rateOffset)
                .finalInterestRate(finalRate)
                .eligibleEmi(eligibleEmi)
                .reasons(String.join("\n", reasons))
                .requestSnapshot(toJson(request))
                .createdBy(staffUserId)
                .createdAt(LocalDateTime.now())
                .build();
        for (RuleCheck check : checks) {
            ResolvedRule rule = check.rule();
            assessment.addRuleResult(EligibilityRuleResult.builder()
                    .ruleId(rule.rule().getId())
                    .paramName(truncate(rule.param().ref().name(), 100))
                    .ruleText(truncate(rule.sentence(), 255))
                    .decisionName(truncate(rule.decisionRef().name(), 50))
                    .rateOffset(rule.rule().getRateOffset())
                    .actualValue(check.actualValue() == null ? null : truncate(display(check.actualValue()), 100))
                    .result(check.outcome().name())
                    .message(truncate(check.message(), 255))
                    .build());
        }
        assessment = assessmentRepository.save(assessment);

        logger.info("Eligibility check {} lead {}: {} (FOIR {}, DTI {}, LTV {}, CIBIL {}), eligible {} at {}%",
                assessment.getId(), assessment.getLeadId(), decision, assessment.getFoir(), assessment.getDti(),
                assessment.getLtv(), assessment.getCibilScore(), eligibleAmount, finalRate);

        EligibilityResponse response = toResponse(assessment);
        response.setRatios(ratios(metrics, checks, rules));
        response.setWarnings(warnings);
        return response;
    }

    /**
     * Value of every rule parameter: calculated metrics, or values sent in additionalFacts.
     */
    private Map<Long, Object> valuesFor(EligibilityCalculator.Inputs in, List<ResolvedRule> rules, BigDecimal amount,
                                        Map<String, String> facts, List<String> warnings) {
        Map<RuleMetric, BigDecimal> metrics = calculator.metrics(in, amount);
        Map<Long, Object> values = new HashMap<>();
        for (ResolvedRule rule : rules) {
            Long paramId = rule.param().ref().id();
            if (values.containsKey(paramId)) {
                continue;
            }
            if (rule.param().metric() != null) {
                BigDecimal value = metrics.get(rule.param().metric());
                if (value != null) {
                    values.put(paramId, value);
                }
                continue;
            }
            String fact = facts.get(normalize(rule.param().ref().code()));
            if (fact == null) {
                fact = facts.get(normalize(rule.param().ref().name()));
            }
            if (fact == null) {
                continue;
            }
            if (rule.param().isChoice()) {
                values.put(paramId, fact);
            } else {
                try {
                    values.put(paramId, new BigDecimal(fact.trim()));
                } catch (NumberFormatException e) {
                    if (warnings != null) {
                        warnings.add("Value '" + fact + "' for " + rule.param().ref().name() + " is not a number; its rules were not checked.");
                    }
                }
            }
        }
        return values;
    }

    /**
     * Highest whole amount (below the requested amount) that no FOIR / DTI / LTV rule rejects.
     * These ratios grow with the amount, so a binary search finds the limit. Rounded down to the
     * nearest Rs. 1,000 when that still passes.
     */
    BigDecimal maxPassingAmount(EligibilityCalculator.Inputs in, List<ResolvedRule> rules, Map<String, String> facts) {
        List<ResolvedRule> amountRules = rules.stream()
                .filter(r -> r.param().metric() != null && r.param().metric().isDependsOnLoanAmount())
                .toList();
        if (!passesAt(in, amountRules, BigDecimal.ZERO, facts)) {
            return BigDecimal.ZERO;
        }
        long low = 0;
        long high = in.requestedAmount().setScale(0, RoundingMode.CEILING).longValueExact();
        while (high - low > 1) {
            long mid = low + (high - low) / 2;
            if (passesAt(in, amountRules, BigDecimal.valueOf(mid), facts)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        BigDecimal amount = BigDecimal.valueOf(low);
        BigDecimal rounded = amount.divide(THOUSAND, 0, RoundingMode.FLOOR).multiply(THOUSAND);
        return rounded.signum() > 0 && passesAt(in, amountRules, rounded, facts) ? rounded : amount;
    }

    private boolean passesAt(EligibilityCalculator.Inputs in, List<ResolvedRule> amountRules, BigDecimal amount,
                             Map<String, String> facts) {
        return !RuleEvaluator.hasOutcome(ruleEvaluator.evaluate(amountRules, valuesFor(in, amountRules, amount, facts, null)),
                Outcome.REJECTED);
    }

    /** Status column of the "Financial ratio" table. */
    private List<EligibilityResponse.Ratio> ratios(Map<RuleMetric, BigDecimal> metrics, List<RuleCheck> checks,
                                                   List<ResolvedRule> rules) {
        List<EligibilityResponse.Ratio> ratios = new ArrayList<>();
        for (RuleMetric metric : RuleMetric.values()) {
            BigDecimal value = metrics.get(metric);
            List<RuleCheck> metricChecks = checks.stream().filter(c -> c.rule().param().metric() == metric).toList();
            String name = rules.stream().filter(r -> r.param().metric() == metric).map(r -> r.param().ref().name())
                    .findFirst().orElse(defaultName(metric));
            String status;
            if (value == null) {
                status = "Not applicable";
            } else if (metricChecks.isEmpty()) {
                status = "No rule";
            } else if (metricChecks.stream().anyMatch(c -> c.outcome() == Outcome.REJECTED)) {
                status = "Outside limit";
            } else if (metricChecks.stream().anyMatch(c -> c.outcome() == Outcome.REFERRED)) {
                status = "Refer for review";
            } else if (metricChecks.stream().anyMatch(c -> c.outcome() == Outcome.OVERRIDDEN)) {
                status = "Rate +" + ResolvedRule.plain(RuleEvaluator.totalRateOffset(metricChecks)) + "%";
            } else {
                status = "Within limit";
            }
            ratios.add(EligibilityResponse.Ratio.builder().code(metric.name()).name(name).value(value).status(status).build());
        }
        return ratios;
    }

    private static String defaultName(RuleMetric metric) {
        return switch (metric) {
            case FOIR -> "FOIR";
            case DTI -> "DTI";
            case LTV -> "LTV";
            case CIBIL_SCORE -> "CIBIL Score";
        };
    }

    private static void addNotCheckedWarnings(List<RuleCheck> checks, EligibilityCalculator.Inputs in, List<String> warnings) {
        Set<String> names = new LinkedHashSet<>();
        boolean ltvSkipped = false;
        for (RuleCheck c : checks) {
            if (c.outcome() != Outcome.NOT_APPLICABLE) {
                continue;
            }
            if (c.rule().param().metric() == RuleMetric.LTV) {
                ltvSkipped = true;
            } else {
                names.add(c.rule().param().ref().name());
            }
        }
        if (ltvSkipped && !in.ltvApplicable()) {
            warnings.add("No security value was given, so the LTV rules were not checked.");
        }
        if (!names.isEmpty()) {
            warnings.add("Rules on " + String.join(", ", names) + " were not checked: no value was sent for them "
                    + "(send them in additionalFacts).");
        }
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public EligibilityResponse getById(Long id) {
        return assessmentRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new EligibilityNotFoundException("Eligibility check not found with ID: " + id));
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public Map<String, Object> list(String leadId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<EligibilityAssessment> result = leadId == null || leadId.isBlank()
                ? assessmentRepository.findAllByOrderByCreatedAtDesc(pageable)
                : assessmentRepository.findByLeadIdOrderByCreatedAtDesc(leadId.trim(), pageable);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent().stream().map(this::toResponse).toList());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("last", result.isLast());
        return body;
    }

    private EligibilityResponse toResponse(EligibilityAssessment a) {
        List<String> reasons = a.getReasons() == null || a.getReasons().isBlank() ? List.of() : List.of(a.getReasons().split("\n"));
        String message = ELIGIBLE.equals(a.getDecision()) && reasons.isEmpty()
                ? "Customer is eligible for the requested loan."
                : reasons.isEmpty() ? DISPLAY.get(a.getDecision()) : reasons.get(0);
        return EligibilityResponse.builder()
                .assessmentId(a.getId())
                .leadId(a.getLeadId())
                .applicantName(a.getApplicantName())
                .productName(a.getProductName())
                .decision(DISPLAY.getOrDefault(a.getDecision(), a.getDecision()))
                .decisionCode(a.getDecision())
                .eligible(ELIGIBLE.equals(a.getDecision()))
                .message(message)
                .requestedAmount(a.getRequestedAmount())
                .tenureMonths(a.getTenureMonths())
                .annualInterestRate(a.getAnnualInterestRate())
                .rateOffset(a.getRateOffset())
                .finalInterestRate(a.getFinalInterestRate())
                .eligibleAmount(a.getEligibleAmount())
                .eligibleEmi(a.getEligibleEmi())
                .proposedEmi(a.getProposedEmi())
                .totalIncome(a.getTotalIncome())
                .totalExpenses(a.getTotalExpenses())
                .netIncome(a.getNetIncome())
                .existingMonthlyDebt(a.getExistingMonthlyDebt())
                .securityValue(a.getSecurityValue())
                .foir(a.getFoir())
                .dti(a.getDti())
                .ltv(a.getLtv())
                .cibilScore(a.getCibilScore())
                .reasons(reasons)
                .ruleResults(a.getRuleResults().stream().map(r -> EligibilityResponse.RuleResultDto.builder()
                        .ruleId(r.getRuleId())
                        .param(r.getParamName())
                        .rule(r.getRuleText())
                        .decision(r.getDecisionName())
                        .rateOffset(r.getRateOffset())
                        .actualValue(r.getActualValue())
                        .result(r.getResult())
                        .message(r.getMessage())
                        .build()).toList())
                .assessedAt(a.getCreatedAt())
                .assessedBy(a.getCreatedBy())
                .build();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Map<String, String> normalizedFacts(Map<String, String> facts) {
        Map<String, String> map = new HashMap<>();
        if (facts != null) {
            facts.forEach((k, v) -> {
                if (k != null && v != null && !v.isBlank()) {
                    map.put(normalize(k), v.trim());
                }
            });
        }
        return map;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static String display(Object value) {
        return value instanceof BigDecimal number ? ResolvedRule.plain(number) : String.valueOf(value);
    }

    /** Indian grouping: 4000000 -> "40,00,000", 1234567.5 -> "12,34,567.50". */
    static String money(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        String sign = value.signum() < 0 ? "-" : "";
        String[] parts = value.abs().toPlainString().split("\\.");
        String digits = parts[0];
        StringBuilder grouped = new StringBuilder();
        if (digits.length() > 3) {
            String head = digits.substring(0, digits.length() - 3);
            for (int i = 0; i < head.length(); i++) {
                if (i > 0 && (head.length() - i) % 2 == 0) {
                    grouped.append(',');
                }
                grouped.append(head.charAt(i));
            }
            grouped.append(',').append(digits.substring(digits.length() - 3));
        } else {
            grouped.append(digits);
        }
        String paise = parts.length > 1 && !parts[1].equals("00") ? "." + parts[1] : "";
        return sign + grouped + paise;
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
