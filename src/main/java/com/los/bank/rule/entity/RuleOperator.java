package com.los.bank.rule.entity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Meaning of a "Rule Operator" lookup ("When value is"), found from its name:
 * Below, Above, Between, Is (and At least / At most). BETWEEN includes both ends;
 * "Above 50" means more than 50, so exactly 50 does not match it.
 */
public enum RuleOperator {

    LESS_THAN("lt", List.of("BELOW", "LESS_THAN", "LT", "<", "UNDER")),
    LESS_THAN_OR_EQUAL("lte", List.of("AT_MOST", "LESS_THAN_OR_EQUAL", "LESS_THAN_OR_EQUAL_TO", "UP_TO", "UPTO", "LTE", "<=")),
    GREATER_THAN("gt", List.of("ABOVE", "GREATER_THAN", "GT", ">", "OVER", "MORE_THAN")),
    GREATER_THAN_OR_EQUAL("gte", List.of("AT_LEAST", "GREATER_THAN_OR_EQUAL", "GREATER_THAN_OR_EQUAL_TO", "GTE", ">=")),
    BETWEEN("between", List.of("BETWEEN", "RANGE", "FROM_TO")),
    EQUALS("eq", List.of("IS", "EQUALS", "EQUAL", "EQUAL_TO", "EQ", "=", "==", "IN"));

    /** Short code used by the Rule Engine screen (lt / gt / between / eq ...). */
    private final String shortCode;
    private final List<String> aliases;

    RuleOperator(String shortCode, List<String> aliases) {
        this.shortCode = shortCode;
        this.aliases = aliases;
    }

    public String getShortCode() { return shortCode; }

    /**
     * From a lookup name ("Below", "Greater than") or a screen code ("lt", "between").
     */
    public static Optional<RuleOperator> fromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String key = text.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        return Arrays.stream(values())
                .filter(o -> o.name().equals(key) || o.aliases.contains(key) || o.shortCode.equalsIgnoreCase(key))
                .findFirst();
    }

    public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal from, BigDecimal to) {
        return switch (this) {
            case LESS_THAN -> actual.compareTo(value) < 0;
            case LESS_THAN_OR_EQUAL -> actual.compareTo(value) <= 0;
            case GREATER_THAN -> actual.compareTo(value) > 0;
            case GREATER_THAN_OR_EQUAL -> actual.compareTo(value) >= 0;
            case BETWEEN -> actual.compareTo(from) >= 0 && actual.compareTo(to) <= 0;
            case EQUALS -> actual.compareTo(value) == 0;
        };
    }
}
