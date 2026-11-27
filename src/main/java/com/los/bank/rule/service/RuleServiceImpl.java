package com.los.bank.rule.service;

import com.los.bank.lookup.entity.SubLookup;
import com.los.bank.rule.dto.RuleLookupValue;
import com.los.bank.rule.dto.RuleOptionsDto;
import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.entity.Rule;
import com.los.bank.rule.entity.RuleDecision;
import com.los.bank.rule.entity.RuleOperator;
import com.los.bank.rule.exception.RuleNotFoundException;
import com.los.bank.rule.exception.RuleValidationException;
import com.los.bank.rule.repository.RuleRepository;
import com.los.bank.rule.service.RuleLookupService.DecisionInfo;
import com.los.bank.rule.service.RuleLookupService.LookupRef;
import com.los.bank.rule.service.RuleLookupService.OperatorInfo;
import com.los.bank.rule.service.RuleLookupService.ParamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Loan eligibility rules (table e001). Parameter, operator and decision are Lookup Master values;
 * rules take effect from their effective date without an approval step.
 */
@Service
@Transactional(transactionManager = "bankTransactionManager")
public class RuleServiceImpl implements RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleServiceImpl.class);
    private static final int MAX_MESSAGE = 255;
    private static final BigDecimal MAX_VALUE = new BigDecimal("9999999999");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final RuleRepository ruleRepository;
    private final RuleLookupService lookups;

    public RuleServiceImpl(RuleRepository ruleRepository, RuleLookupService lookups) {
        this.ruleRepository = ruleRepository;
        this.lookups = lookups;
    }

    // ---------------------------------------------------------------------
    // Options
    // ---------------------------------------------------------------------

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public RuleOptionsDto getOptions() {
        return RuleOptionsDto.builder()
                .parameters(lookups.parameters().stream().map(p -> RuleOptionsDto.Parameter.builder()
                        .id(p.ref().id()).code(p.ref().code()).name(p.ref().name())
                        .unit(p.unit())
                        .metric(p.metric() != null ? p.metric().name() : null)
                        .choice(p.isChoice())
                        .options(p.isChoice() ? p.options().stream().map(RuleServiceImpl::value).toList() : null)
                        .build()).toList())
                .operators(lookups.operators().stream().map(o -> RuleOptionsDto.Operator.builder()
                        .id(o.ref().id()).code(o.ref().code()).name(o.ref().name())
                        .type(o.operator() != null ? o.operator().name() : null)
                        .shortCode(o.operator() != null ? o.operator().getShortCode() : null)
                        .build()).toList())
                .decisions(lookups.decisions().stream().map(d -> RuleOptionsDto.Decision.builder()
                        .id(d.ref().id()).code(d.ref().code()).name(d.ref().name())
                        .type(d.decision() != null ? d.decision().name() : null)
                        .needsRateOffset(d.decision() == RuleDecision.OVERRIDE)
                        .build()).toList())
                .build();
    }

    // ---------------------------------------------------------------------
    // Create / update / status / delete
    // ---------------------------------------------------------------------

    @Override
    public RuleResponseDto createRule(RuleRequestDto request, Long staffUserId) {
        Rule rule = new Rule();
        ResolvedRule resolved = applyRequest(rule, request, true);
        checkOverlap(resolved);
        rule.setCreatedBy(staffUserId);
        rule.setCreatedAt(LocalDateTime.now());
        Rule saved = ruleRepository.save(rule);
        logger.info("Rule {} created: {}", saved.getId(), resolved.sentence());
        return toResponse(new ResolvedRule(saved, resolved.param(), resolved.operatorRef(), resolved.operator(),
                resolved.decisionRef(), resolved.decision(), resolved.choice()));
    }

    @Override
    public RuleResponseDto updateRule(Long id, RuleRequestDto request, Long staffUserId) {
        Rule rule = findRule(id);
        ResolvedRule resolved = applyRequest(rule, request, false);
        checkOverlap(resolved);
        rule.setUpdatedBy(staffUserId);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);
        logger.info("Rule {} updated: {}", id, resolved.sentence());
        return toResponse(resolved);
    }

    @Override
    public RuleResponseDto changeStatus(Long id, String status, Long staffUserId) {
        Rule rule = findRule(id);
        rule.setStatus(normalizeStatus(status, null, null));
        ResolvedRule resolved = resolve(List.of(rule)).get(0);
        checkOverlap(resolved);
        rule.setUpdatedBy(staffUserId);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);
        return toResponse(resolved);
    }

    @Override
    public void deleteRule(Long id) {
        ruleRepository.delete(findRule(id));
        logger.info("Rule {} deleted", id);
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public RuleResponseDto getRuleById(Long id) {
        return toResponse(resolve(List.of(findRule(id))).get(0));
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public Map<String, Object> getAllRules(int page, int size, String status, String param, String decision) {
        String statusFilter = isBlank(status) ? null : normalizeStatus(status, null, null);
        List<ResolvedRule> all = resolve(ruleRepository.findAll());

        String paramKey = isBlank(param) ? null : RuleLookupService.normalize(param);
        String decisionKey = isBlank(decision) ? null : RuleLookupService.normalize(decision);
        List<ResolvedRule> filtered = all.stream()
                .filter(r -> statusFilter == null || statusFilter.equals(r.rule().getStatus()))
                .filter(r -> paramKey == null || matchesRef(r.param().ref(), paramKey))
                .filter(r -> decisionKey == null || matchesRef(r.decisionRef(), decisionKey))
                .sorted(Comparator.comparing((ResolvedRule r) -> r.param().ref().name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(r -> r.rule().getId()))
                .toList();

        int pageSize = Math.min(Math.max(1, size), 200);
        int pageIndex = Math.max(0, page);
        int fromIndex = Math.min(pageIndex * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("totalElements", filtered.size());
        result.put("totalPages", (filtered.size() + pageSize - 1) / pageSize);
        result.put("currentPage", pageIndex);
        result.put("pageSize", pageSize);
        result.put("data", filtered.subList(fromIndex, toIndex).stream().map(this::toResponse).toList());
        return result;
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public List<ResolvedRule> getRulesInForce(LocalDate date) {
        return resolve(ruleRepository.findRulesInForce(date));
    }

    // ---------------------------------------------------------------------
    // Request -> rule
    // ---------------------------------------------------------------------

    private ResolvedRule applyRequest(Rule rule, RuleRequestDto request, boolean isCreate) {
        if (request == null) {
            throw new RuleValidationException("Request payload cannot be null.");
        }
        Map<String, String> errors = new LinkedHashMap<>();

        ParamInfo param = null;
        if (isBlank(request.getParam())) {
            errors.put("param", "Check (parameter) is required.");
        } else {
            param = lookups.findParameter(request.getParam()).orElse(null);
            if (param == null) {
                errors.put("param", "'" + request.getParam() + "' is not an active value of the Rule Parameter lookup.");
            }
        }

        OperatorInfo operator = null;
        if (param != null && param.isChoice() && isBlank(request.getOp())) {
            operator = lookups.findOperator("eq").orElse(null);
            if (operator == null) {
                errors.put("op", "The Rule Operator lookup needs an \"Is\" value for choice parameters.");
            }
        } else if (isBlank(request.getOp())) {
            errors.put("op", "\"When value is\" (operator) is required.");
        } else {
            operator = lookups.findOperator(request.getOp()).orElse(null);
            if (operator == null) {
                errors.put("op", "'" + request.getOp() + "' is not an active value of the Rule Operator lookup.");
            } else if (operator.operator() == null) {
                errors.put("op", "Operator '" + operator.ref().name() + "' is not understood. Use names like Below, "
                        + "Above, Between, Is, At least, At most.");
            }
        }

        DecisionInfo decision = null;
        if (isBlank(request.getDecision())) {
            errors.put("decision", "\"Then\" (decision) is required.");
        } else {
            decision = lookups.findDecision(request.getDecision()).orElse(null);
            if (decision == null) {
                errors.put("decision", "'" + request.getDecision() + "' is not an active value of the Rule Decision lookup.");
            } else if (decision.decision() == null) {
                errors.put("decision", "Decision '" + decision.ref().name() + "' is not understood. Use names like "
                        + "Allow, Override, Reject or Refer.");
            }
        }

        BigDecimal value = null;
        BigDecimal from = null;
        BigDecimal to = null;
        LookupRef choice = null;
        if (param != null && param.isChoice()) {
            if (operator != null && operator.operator() != null && operator.operator() != RuleOperator.EQUALS) {
                errors.put("op", param.ref().name() + " is a choice: use \"Is\".");
            }
            if (isBlank(request.getA())) {
                errors.put("a", "Choose a value for " + param.ref().name() + ".");
            } else {
                choice = lookups.findOption(param, request.getA()).orElse(null);
                if (choice == null) {
                    errors.put("a", "'" + request.getA() + "' is not an option of " + param.ref().name() + ". Options: "
                            + String.join(", ", param.options().stream().map(LookupRef::name).toList()) + ".");
                }
            }
        } else if (operator != null && operator.operator() == RuleOperator.BETWEEN) {
            from = number(request.getA(), "a", "From", errors);
            to = number(request.getB(), "b", "To", errors);
            if (from != null && to != null && from.compareTo(to) >= 0) {
                errors.put("a", "'From' must be lower than 'To'.");
            }
        } else if (operator != null && operator.operator() != null) {
            value = number(request.getA(), "a", "Value", errors);
        }
        if (param != null && param.metric() != null) {
            checkPercentRange(param, value, "a", errors);
            checkPercentRange(param, from, "a", errors);
            checkPercentRange(param, to, "b", errors);
        }

        BigDecimal offset = request.getOffset();
        if (decision != null && decision.decision() == RuleDecision.OVERRIDE) {
            if (offset == null) {
                errors.put("offset", "Enter the extra rate to add (\"Add to rate %\") for Override.");
            } else if (offset.signum() < 0 || offset.compareTo(HUNDRED) > 0) {
                errors.put("offset", "Rate offset must be between 0 and 100.");
            } else if (offset.stripTrailingZeros().scale() > 2) {
                errors.put("offset", "Rate offset can have at most 2 decimal places.");
            }
        } else {
            offset = BigDecimal.ZERO;
        }

        String message = trimToNull(request.getMessage());
        if (message != null && message.length() > MAX_MESSAGE) {
            errors.put("message", "Message must be at most " + MAX_MESSAGE + " characters.");
        }

        String status = null;
        try {
            status = normalizeStatus(request.getStatus(), request.getActive(), isCreate ? Rule.ACTIVE : rule.getStatus());
        } catch (RuleValidationException e) {
            errors.put("status", e.getMessage());
        }

        LocalDate effectiveDate = request.getEffectiveDate();
        if (effectiveDate == null) {
            effectiveDate = isCreate || rule.getEffectiveDate() == null ? LocalDate.now() : rule.getEffectiveDate();
        } else if (effectiveDate.isBefore(LocalDate.now())
                && (isCreate || !effectiveDate.equals(rule.getEffectiveDate()))) {
            errors.put("effectiveDate", "Effective date cannot be in the past.");
        }

        if (!errors.isEmpty()) {
            throw new RuleValidationException("Rule validation failed", errors);
        }

        rule.setParamLookupId(param.ref().id());
        rule.setOperatorLookupId(operator.ref().id());
        rule.setDecisionLookupId(decision.ref().id());
        rule.setVal(value);
        rule.setFromValue(from);
        rule.setToValue(to);
        rule.setValueLookupId(choice != null ? choice.id() : null);
        rule.setRateOffset(offset);
        rule.setMessage(message);
        rule.setStatus(status);
        rule.setEffectiveDate(effectiveDate);
        return new ResolvedRule(rule, param, operator.ref(), operator.operator(), decision.ref(), decision.decision(), choice);
    }

    private static BigDecimal number(String text, String field, String label, Map<String, String> errors) {
        if (isBlank(text)) {
            errors.put(field, label + " is required.");
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.abs().compareTo(MAX_VALUE) > 0) {
                errors.put(field, label + " is too large.");
            } else if (value.stripTrailingZeros().scale() > 2) {
                errors.put(field, label + " can have at most 2 decimal places.");
            }
            return value;
        } catch (NumberFormatException e) {
            errors.put(field, label + " must be a number.");
            return null;
        }
    }

    private static void checkPercentRange(ParamInfo param, BigDecimal value, String field, Map<String, String> errors) {
        if (value == null) {
            return;
        }
        BigDecimal max = param.metric().name().equals("CIBIL_SCORE") ? BigDecimal.valueOf(900) : BigDecimal.valueOf(1000);
        if (value.signum() < 0 || value.compareTo(max) > 0) {
            errors.put(field, param.ref().name() + " value must be between 0 and " + max + ".");
        }
    }

    /**
     * Two ACTIVE rules of the same parameter may not cover the same value.
     */
    private void checkOverlap(ResolvedRule candidate) {
        if (!Rule.ACTIVE.equals(candidate.rule().getStatus())) {
            return;
        }
        Long paramId = candidate.rule().getParamLookupId();
        List<Rule> others = ruleRepository.findByParamLookupIdAndStatus(paramId, Rule.ACTIVE).stream()
                .filter(o -> candidate.rule().getId() == null || !candidate.rule().getId().equals(o.getId()))
                .toList();
        if (others.isEmpty()) {
            return;
        }
        for (ResolvedRule other : resolve(others)) {
            boolean overlap;
            if (candidate.choice() != null || other.choice() != null) {
                overlap = candidate.choice() != null && other.choice() != null
                        && candidate.choice().id().equals(other.choice().id());
            } else if (candidate.operator() == null || other.operator() == null) {
                overlap = false;
            } else {
                overlap = Range.of(candidate).overlaps(Range.of(other));
            }
            if (overlap) {
                throw new RuleValidationException("This rule overlaps active rule #" + other.rule().getId() + " ("
                        + other.conditionText() + " -> " + other.decisionRef().name()
                        + "). Change the values or deactivate that rule first.");
            }
        }
    }

    /** Values a numeric rule matches, as an interval; null bound = unbounded. */
    record Range(BigDecimal low, boolean lowInclusive, BigDecimal high, boolean highInclusive) {

        static Range of(ResolvedRule r) {
            Rule rule = r.rule();
            BigDecimal v = rule.getVal();
            return switch (r.operator()) {
                case LESS_THAN -> new Range(null, false, v, false);
                case LESS_THAN_OR_EQUAL -> new Range(null, false, v, true);
                case GREATER_THAN -> new Range(v, false, null, false);
                case GREATER_THAN_OR_EQUAL -> new Range(v, true, null, false);
                case BETWEEN -> new Range(rule.getFromValue(), true, rule.getToValue(), true);
                case EQUALS -> new Range(v, true, v, true);
            };
        }

        boolean overlaps(Range other) {
            BigDecimal lo;
            boolean loInc;
            if (low == null) {
                lo = other.low;
                loInc = other.lowInclusive;
            } else if (other.low == null) {
                lo = low;
                loInc = lowInclusive;
            } else {
                int c = low.compareTo(other.low);
                lo = c >= 0 ? low : other.low;
                loInc = c > 0 ? lowInclusive : c < 0 ? other.lowInclusive : lowInclusive && other.lowInclusive;
            }
            BigDecimal hi;
            boolean hiInc;
            if (high == null) {
                hi = other.high;
                hiInc = other.highInclusive;
            } else if (other.high == null) {
                hi = high;
                hiInc = highInclusive;
            } else {
                int c = high.compareTo(other.high);
                hi = c <= 0 ? high : other.high;
                hiInc = c < 0 ? highInclusive : c > 0 ? other.highInclusive : highInclusive && other.highInclusive;
            }
            if (lo == null || hi == null) {
                return true;
            }
            int c = lo.compareTo(hi);
            return c < 0 || (c == 0 && loInc && hiInc);
        }
    }

    // ---------------------------------------------------------------------
    // Stored rule -> resolved rule -> response
    // ---------------------------------------------------------------------

    List<ResolvedRule> resolve(List<Rule> rules) {
        List<Long> ids = new ArrayList<>();
        for (Rule r : rules) {
            ids.add(r.getParamLookupId());
            ids.add(r.getOperatorLookupId());
            ids.add(r.getDecisionLookupId());
            ids.add(r.getValueLookupId());
        }
        Map<Long, SubLookup> subs = lookups.loadByIds(ids);
        Map<Long, ParamInfo> params = new HashMap<>();
        List<ResolvedRule> resolved = new ArrayList<>();
        for (Rule r : rules) {
            ParamInfo param = params.computeIfAbsent(r.getParamLookupId(), id -> {
                SubLookup sub = subs.get(id);
                return sub != null ? lookups.toParamInfo(sub)
                        : new ParamInfo(new LookupRef(id, null, "Parameter #" + id), null, "", List.of());
            });
            SubLookup op = subs.get(r.getOperatorLookupId());
            SubLookup dec = subs.get(r.getDecisionLookupId());
            SubLookup choice = r.getValueLookupId() != null ? subs.get(r.getValueLookupId()) : null;
            resolved.add(new ResolvedRule(r, param,
                    op != null ? LookupRef.of(op) : new LookupRef(r.getOperatorLookupId(), null, "?"),
                    op != null ? RuleOperator.fromText(op.getSubTypeDescription()).orElse(null) : null,
                    dec != null ? LookupRef.of(dec) : new LookupRef(r.getDecisionLookupId(), null, "?"),
                    dec != null ? RuleDecision.fromText(dec.getSubTypeDescription()).orElse(null) : null,
                    choice != null ? LookupRef.of(choice) : null));
        }
        return resolved;
    }

    private RuleResponseDto toResponse(ResolvedRule r) {
        Rule rule = r.rule();
        boolean active = Rule.ACTIVE.equals(rule.getStatus());
        boolean between = r.operator() == RuleOperator.BETWEEN;
        String a = r.choice() != null ? r.choice().name()
                : between ? ResolvedRule.plain(rule.getFromValue()) : ResolvedRule.plain(rule.getVal());
        return RuleResponseDto.builder()
                .id(rule.getId())
                .param(r.param().ref().name())
                .op(r.operator() != null ? r.operator().getShortCode() : null)
                .a(a)
                .b(between ? ResolvedRule.plain(rule.getToValue()) : null)
                .decision(r.decisionRef().name())
                .offset(rule.getRateOffset())
                .active(active)
                .effectiveDate(rule.getEffectiveDate())
                .parameter(value(r.param().ref()))
                .operator(value(r.operatorRef()))
                .decisionLookup(value(r.decisionRef()))
                .choiceValue(r.choice() != null ? value(r.choice()) : null)
                .metric(r.param().metric() != null ? r.param().metric().name() : null)
                .unit(r.param().unit())
                .operatorType(r.operator() != null ? r.operator().name() : null)
                .decisionType(r.decision() != null ? r.decision().name() : null)
                .value(rule.getVal())
                .from(rule.getFromValue())
                .to(rule.getToValue())
                .message(rule.getMessage())
                .description(r.sentence())
                .status(rule.getStatus())
                .inForce(active && rule.getEffectiveDate() != null && !rule.getEffectiveDate().isAfter(LocalDate.now()))
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .createdBy(rule.getCreatedBy())
                .updatedBy(rule.getUpdatedBy())
                .build();
    }

    private static RuleLookupValue value(LookupRef ref) {
        return RuleLookupValue.builder().id(ref.id()).code(ref.code()).name(ref.name()).build();
    }

    private static boolean matchesRef(LookupRef ref, String normalizedKey) {
        return normalizedKey.equals(RuleLookupService.normalize(ref.name()))
                || normalizedKey.equals(RuleLookupService.normalize(ref.code()))
                || normalizedKey.equals(String.valueOf(ref.id()));
    }

    private Rule findRule(Long id) {
        if (id == null) {
            throw new RuleValidationException("Rule ID cannot be null.");
        }
        return ruleRepository.findById(id).orElseThrow(() -> new RuleNotFoundException("Rule not found with id: " + id));
    }

    private static String normalizeStatus(String status, Boolean active, String defaultStatus) {
        if (isBlank(status)) {
            if (active != null) {
                return active ? Rule.ACTIVE : Rule.INACTIVE;
            }
            if (defaultStatus != null) {
                return defaultStatus;
            }
            throw new RuleValidationException("Status is required: ACTIVE or INACTIVE.");
        }
        String upper = status.trim().toUpperCase(Locale.ROOT);
        if (!Rule.ACTIVE.equals(upper) && !Rule.INACTIVE.equals(upper)) {
            throw new RuleValidationException("Invalid status '" + status + "'. Use ACTIVE or INACTIVE.");
        }
        return upper;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
