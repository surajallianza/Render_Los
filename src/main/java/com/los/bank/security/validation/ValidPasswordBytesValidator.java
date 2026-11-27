package com.los.bank.security.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * Validator implementation that checks if a string's UTF-8 byte length exceeds the maximum limit.
 * Never truncates passwords.
 */
public class ValidPasswordBytesValidator implements ConstraintValidator<ValidPasswordBytes, String> {

    private int max;

    @Override
    public void initialize(ValidPasswordBytes constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Null/empty checks are handled by @NotBlank or optional constraints
        }
        return value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
