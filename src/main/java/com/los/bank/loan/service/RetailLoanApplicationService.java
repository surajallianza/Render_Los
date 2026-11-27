package com.los.bank.loan.service;

import com.los.bank.loan.dto.RetailLoanFormDto;

import java.util.Map;

/**
 * Service interface for Retail Loan Application CRUD and business operations.
 */
public interface RetailLoanApplicationService {

    RetailLoanFormDto createApplication(RetailLoanFormDto form);

    Map<String, Object> getAllApplications(int page, int size, String status, String search);

    RetailLoanFormDto getApplicationById(Long id);

    RetailLoanFormDto updateApplication(Long id, RetailLoanFormDto form);
}
