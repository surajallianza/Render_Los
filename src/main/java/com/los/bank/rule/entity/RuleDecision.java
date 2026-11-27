package com.los.bank.rule.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Meaning of a "Rule Decision" lookup ("Then"), found from its name.
 * <ul>
 *   <li>ALLOW: the check passes</li>
 *   <li>OVERRIDE: the check passes and the rule's rate offset is added to the interest rate</li>
 *   <li>REFER: needs manual review</li>
 *   <li>REJECT: not eligible</li>
 * </ul>
 */
public enum RuleDecision {

    ALLOW(List.of("ALLOW", "PASS", "APPROVE", "ACCEPT", "ELIGIBLE")),
    OVERRIDE(List.of("OVERRIDE", "RATE_OVERRIDE", "OVERRIDE_RATE", "ADD_RATE")),
    REFER(List.of("REFER", "REVIEW", "REFER_FOR_REVIEW", "MANUAL_REVIEW")),
    REJECT(List.of("REJECT", "DECLINE", "DENY", "NOT_ELIGIBLE"));

    private final List<String> aliases;

    RuleDecision(List<String> aliases) {
        this.aliases = aliases;
    }

    public static Optional<RuleDecision> fromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String key = text.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]+", "_").replaceAll("^_+|_+$", "");
        return Arrays.stream(values()).filter(d -> d.name().equals(key) || d.aliases.contains(key)).findFirst();
    }
}
