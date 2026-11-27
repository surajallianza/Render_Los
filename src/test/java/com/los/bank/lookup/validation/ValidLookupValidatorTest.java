package com.los.bank.lookup.validation;

import com.los.bank.lookup.resolver.LookupResolver;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidLookupValidatorTest {

    @Mock
    private LookupResolver lookupResolver;

    @Mock
    private ValidLookup validLookupAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private ValidLookupValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValidLookupValidator(lookupResolver);
        when(validLookupAnnotation.lookupCode()).thenReturn("10004");
        when(validLookupAnnotation.message()).thenReturn("Invalid or inactive value for lookup");
        validator.initialize(validLookupAnnotation);
    }

    @Test
    @DisplayName("isValid: null or blank is valid (optional field)")
    void isValid_nullOrBlank_isValid() {
        assertTrue(validator.isValid(null, context));
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
        verify(lookupResolver, never()).isValid(anyString(), anyString());
    }

    @Test
    @DisplayName("isValid: valid code returns true")
    void isValid_validCode_returnsTrue() {
        when(lookupResolver.isValid("10004", "1")).thenReturn(true);
        assertTrue(validator.isValid("1", context));
    }

    @Test
    @DisplayName("isValid: invalid code returns false and builds custom message")
    void isValid_invalidCode_returnsFalse() {
        when(lookupResolver.isValid("10004", "99")).thenReturn(false);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        boolean result = validator.isValid("99", context);

        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Invalid or inactive value '99' for lookup 10004");
        verify(violationBuilder).addConstraintViolation();
    }
}
