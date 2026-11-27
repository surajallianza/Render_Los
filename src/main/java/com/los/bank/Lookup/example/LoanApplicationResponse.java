package com.los.bank.lookup.example;

import com.los.bank.lookup.dto.LookupValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Example Response DTO demonstrating how response DTOs return both code and human-readable name:
 * e.g., { "loanType": { "code": "1", "name": "Gold Loan" } }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationResponse {

    private Long id;
    private String customerId;
    private BigDecimal amount;
    private Long loanTypeLookupId; // Stored m002.id in database
    private LookupValue loanType;  // Formatted response for frontend/API consumer
    private LookupValue repaymentType;
}
