package com.los.bank.lookup.example;

import com.los.bank.lookup.dto.LookupValue;
import com.los.bank.lookup.resolver.LookupResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Example Service demonstrating how other modules (such as Loan Origination)
 * use LookupResolver to resolve submitted codes to database IDs (m002.id) before persisting.
 */
@Service
public class LoanApplicationExampleService {

    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationExampleService.class);
    private static final String LOOKUP_LOAN_TYPE = "10004";
    private static final String LOOKUP_REPAYMENT_TYPE = "10005";

    private final LookupResolver lookupResolver;

    public LoanApplicationExampleService(LookupResolver lookupResolver) {
        this.lookupResolver = lookupResolver;
    }

    /**
     * Demonstrates saving a loan application:
     * 1. Request DTO is validated via @Valid (calling @ValidLookup)
     * 2. Service resolves submitted code "1" -> m002.id (e.g. 42L)
     * 3. Entity stores the resolved Long ID
     * 4. Response maps back to { "code": "1", "name": "Gold Loan" }
     */
    public LoanApplicationResponse processLoanApplication(LoanApplicationCreateRequest request) {
        // Resolve submitted code to m002.id (throws 400 if invalid or inactive)
        Long resolvedLoanTypeId = lookupResolver.resolveId(LOOKUP_LOAN_TYPE, request.getLoanType());
        logger.info("Resolved loan type code '{}' to m002.id={}", request.getLoanType(), resolvedLoanTypeId);

        Long resolvedRepaymentId = null;
        if (request.getRepaymentType() != null && !request.getRepaymentType().isBlank()) {
            resolvedRepaymentId = lookupResolver.resolveId(LOOKUP_REPAYMENT_TYPE, request.getRepaymentType());
        }

        // Simulating entity save with resolved foreign key ID
        Long generatedLoanApplicationId = 1001L;

        // Build Response DTO with formatted LookupValue helpers
        LookupValue loanTypeValue = lookupResolver.toLookupValue(LOOKUP_LOAN_TYPE, request.getLoanType());
        LookupValue repaymentValue = request.getRepaymentType() != null
                ? lookupResolver.toLookupValue(LOOKUP_REPAYMENT_TYPE, request.getRepaymentType())
                : null;

        return LoanApplicationResponse.builder()
                .id(generatedLoanApplicationId)
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .loanTypeLookupId(resolvedLoanTypeId)
                .loanType(loanTypeValue)
                .repaymentType(repaymentValue)
                .build();
    }
}
