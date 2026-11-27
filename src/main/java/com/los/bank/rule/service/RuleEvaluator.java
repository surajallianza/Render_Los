package com.los.bank.rule.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks values against rules. No database access, so any module can use it with its own values.
 */
@Component
public class RuleEvaluator {

    public enum Outcome {
        /** Matched an Allow rule. */
        ALLOWED,
        /** Matched an Override rule: passes, rate offset added. */
        OVERRIDDEN,
        /** Matched a Refer rule: needs manual review. */
        REFERRED,
        /** Matched a Reject rule. */
        REJECTED,
        /** Value is outside the rule. */
        NOT_MATCHED,
        /** No value for this parameter (e.g. LTV without a security value). */
        NOT_APPLICABLE
    }

    public record RuleCheck(ResolvedRule rule, Object actualValue, Outcome outcome, String message) {
        public BigDecimal rateOffset() {
            return outcome == Outcome.OVERRIDDEN && rule.rule().getRateOffset() != null
                    ? rule.rule().getRateOffset() : BigDecimal.ZERO;
        }
    }

    /**
     * @param values value per parameter lookup id: BigDecimal for numeric parameters, String for
     *               choice parameters; a parameter missing from the map is NOT_APPLICABLE
     */
    public List<RuleCheck> evaluate(List<ResolvedRule> rules, Map<Long, Object> values) {
        List<RuleCheck> checks = new ArrayList<>();
        for (ResolvedRule rule : rules) {
            String paramName = rule.param().ref().name();
            Object actual = values.get(rule.param().ref().id());
            if (actual == null) {
                checks.add(new RuleCheck(rule, null, Outcome.NOT_APPLICABLE, paramName + " not available, rule not checked"));
                continue;
            }
            if (!rule.isUsable() || !rule.matches(actual)) {
                checks.add(new RuleCheck(rule, actual, Outcome.NOT_MATCHED, null));
                continue;
            }
            Outcome outcome = switch (rule.decision()) {
                case ALLOW -> Outcome.ALLOWED;
                case OVERRIDE -> Outcome.OVERRIDDEN;
                case REFER -> Outcome.REFERRED;
                case REJECT -> Outcome.REJECTED;
            };
            String message = rule.rule().getMessage() != null ? rule.rule().getMessage() : defaultMessage(rule, actual, outcome);
            checks.add(new RuleCheck(rule, actual, outcome, message));
        }
        return checks;
    }

    private static String defaultMessage(ResolvedRule rule, Object actual, Outcome outcome) {
        String value = actual instanceof BigDecimal number ? ResolvedRule.plain(number) + rule.param().unit() : String.valueOf(actual);
        String condition = rule.param().ref().name() + " " + value + " is " + rule.conditionPart();
        if (rule.choice() != null) {
            condition = rule.param().ref().name() + " is " + rule.choice().name();
        }
        return switch (outcome) {
            case ALLOWED -> condition + " (allowed)";
            case OVERRIDDEN -> condition + ": +" + ResolvedRule.plain(rule.rule().getRateOffset()) + "% added to the interest rate";
            case REFERRED -> condition + ": needs manual review";
            case REJECTED -> condition + " (not allowed)";
            default -> condition;
        };
    }

    public static boolean hasOutcome(List<RuleCheck> checks, Outcome outcome) {
        return checks.stream().anyMatch(c -> c.outcome() == outcome);
    }

    /** Sum of the rate offsets of all matched Override rules. */
    public static BigDecimal totalRateOffset(List<RuleCheck> checks) {
        return checks.stream().map(RuleCheck::rateOffset).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
