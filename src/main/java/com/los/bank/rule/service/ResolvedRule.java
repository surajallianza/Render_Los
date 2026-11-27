package com.los.bank.rule.service;

import com.los.bank.rule.entity.Rule;
import com.los.bank.rule.entity.RuleDecision;
import com.los.bank.rule.entity.RuleOperator;
import com.los.bank.rule.service.RuleLookupService.LookupRef;
import com.los.bank.rule.service.RuleLookupService.ParamInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A rule with its lookup values and their meaning.
 *
 * @param operator null when the operator lookup's name is not understood
 * @param decision null when the decision lookup's name is not understood
 * @param choice   choice value for choice parameters (e.g. Registered)
 */
public record ResolvedRule(Rule rule, ParamInfo param, LookupRef operatorRef, RuleOperator operator,
                           LookupRef decisionRef, RuleDecision decision, LookupRef choice) {

    public boolean isUsable() {
        return operator != null && decision != null;
    }

    /**
     * @param actual BigDecimal for numeric parameters, String (code or name) for choice parameters
     */
    public boolean matches(Object actual) {
        if (choice != null) {
            // Choice rules always use "Is" (enforced when the rule is saved)
            String text = String.valueOf(actual).trim();
            return text.equalsIgnoreCase(choice.code()) || text.equalsIgnoreCase(choice.name());
        }
        if (!(actual instanceof BigDecimal number)) {
            return false;
        }
        return operator.matches(number, rule.getVal(), rule.getFromValue(), rule.getToValue());
    }

    /** "above 50%" / "between 401 and 500" / "Not Registered". */
    public String conditionPart() {
        if (choice != null) {
            return choice.name();
        }
        String unit = param.unit();
        String words = operatorRef != null ? operatorRef.name().toLowerCase() : String.valueOf(operator);
        if (operator == RuleOperator.BETWEEN) {
            return words + " " + plain(rule.getFromValue()) + unit + " and " + plain(rule.getToValue()) + unit;
        }
        return words + " " + plain(rule.getVal()) + unit;
    }

    /** "FOIR above 50%". */
    public String conditionText() {
        return param.ref().name() + " " + conditionPart();
    }

    /** "If FOIR is above 50%, reject the application." */
    public String sentence() {
        String then;
        if (decision == null) {
            then = decisionRef.name();
        } else {
            then = switch (decision) {
                case ALLOW -> "allow the application";
                case OVERRIDE -> "allow it and add +" + plain(rule.getRateOffset()) + "% to the interest rate";
                case REFER -> "refer the application for review";
                case REJECT -> "reject the application";
            };
        }
        return "If " + param.ref().name() + " is " + conditionPart() + ", " + then + ".";
    }

    public static String plain(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal stripped = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }
}
