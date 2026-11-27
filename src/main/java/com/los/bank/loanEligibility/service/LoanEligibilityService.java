package com.los.bank.loanEligibility.service;

import com.los.bank.loanEligibility.dto.EligibilityRequest;
import com.los.bank.loanEligibility.dto.EligibilityResponse;

import java.util.Map;

public interface LoanEligibilityService {

    /**
     * Calculates FOIR, DTI and LTV, checks them and the CIBIL score against the active rules,
     * works out the eligible amount and saves the check.
     */
    EligibilityResponse assess(EligibilityRequest request, Long staffUserId);

    EligibilityResponse getById(Long id);

    Map<String, Object> list(String leadId, int page, int size);
}
