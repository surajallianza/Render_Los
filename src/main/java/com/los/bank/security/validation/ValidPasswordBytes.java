package com.los.bank.security.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation constraint to ensure passwords do not exceed BCrypt's 72-byte limit in UTF-8 encoding.
 */
@Documented
@Constraint(validatedBy = ValidPasswordBytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPasswordBytes {

    String message() default "Password must not exceed 72 bytes (in UTF-8 encoding)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max() default 72;
}
