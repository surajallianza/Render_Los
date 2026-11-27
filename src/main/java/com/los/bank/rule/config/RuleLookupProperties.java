package com.los.bank.rule.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Lookup Master categories behind the Rule Engine dropdowns (prefix app.rule-engine.lookups).
 * Each value is the lookup's name (description) or its code.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rule-engine.lookups")
@Getter
@Setter
public class RuleLookupProperties {

    /** "Check" dropdown: FOIR, Debt to Income (DTI), Loan-to-Value (LTV), CIBIL Score, Age ... */
    private String parameter = "Rule Parameter";

    /** "When value is" dropdown: Below, Between, Above, Is. */
    private String operator = "Rule Operator";

    /** "Then" dropdown: Allow, Override, Reject. */
    private String decision = "Rule Decision";
}
