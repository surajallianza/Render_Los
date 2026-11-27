package com.los.bank.lookup.validation;

import com.los.bank.lookup.resolver.LookupResolver;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Constraint validator executing lookup validation against the persistent/cached lookup tables.
 */
@Component
public class ValidLookupValidator implements ConstraintValidator<ValidLookup, String> {

    private final LookupResolver lookupResolver;
    private String lookupCode;
    private String defaultMessage;

    public ValidLookupValidator(LookupResolver lookupResolver) {
        this.lookupResolver = lookupResolver;
    }

    @Override
    public void initialize(ValidLookup annotation) {
        this.lookupCode = annotation.lookupCode();
        this.defaultMessage = annotation.message();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null or blank strings are considered valid unless paired with @NotBlank / @NotNull
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        boolean valid = lookupResolver.isValid(lookupCode, value.trim());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            String customMessage = String.format("Invalid or inactive value '%s' for lookup %s", value.trim(), lookupCode);
            context.buildConstraintViolationWithTemplate(customMessage).addConstraintViolation();
            return false;
        }

        return true;
    }
}
