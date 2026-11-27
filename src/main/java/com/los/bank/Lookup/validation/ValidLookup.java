package com.los.bank.lookup.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a submitted code exists and is active under the specified parent lookup code.
 * Null and blank strings are considered valid unless paired with @NotBlank / @NotNull.
 */
@Documented
@Constraint(validatedBy = ValidLookupValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLookup {

    /**
     * Parent lookup type code (e.g. "10004" for loan type).
     */
    String lookupCode();

    String message() default "Invalid or inactive value for lookup";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
