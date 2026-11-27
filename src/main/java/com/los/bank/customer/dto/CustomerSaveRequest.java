package com.los.bank.customer.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSaveRequest {

    private String leadId;
    private Long branchId;

    private PersonalRequest personal;
    private IdentityRequest identity;
    private FamilyRequest family;
    private AddressRequest address;
    private EmploymentRequest employment;
    private List<PreviousEmploymentRequest> previousEmployment;
    private IncomeRequest income;
    private BankRequest bank;
    private List<TaxReturnRequest> taxReturns;
    private List<LoanHistoryRequest> loanHistory;
    private List<ReferenceRequest> references;
}
