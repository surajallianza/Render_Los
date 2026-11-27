package com.los.bank.lookup.example;

import com.los.bank.lookup.validation.ValidLookup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Example Request DTO showing how other LOS forms (loan application, customer onboarding)
 * use @ValidLookup to validate incoming sub lookup codes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationCreateRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Validates that the submitted code exists and is active under Lookup '10004' (Loan Types).
     */
    @NotBlank(message = "Loan type is required")
    @ValidLookup(lookupCode = "10004", message = "Invalid or inactive loan type selected")
    private String loanType;

    /**
     * Optional lookup field: null/blank is permitted unless provided.
     */
    @ValidLookup(lookupCode = "10005", message = "Invalid or inactive repayment type")
    private String repaymentType;
}
