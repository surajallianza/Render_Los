package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerProfileResponse {

    private String customerCode;
    private Long customerId;
    private Long branchId;
    private Boolean isActive;
    /** DRAFT while the onboarding wizard is in progress, COMPLETED after submit. */
    private String onboardingStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PersonalResponse personal;
    private IdentityResponse identity;
    private FamilyResponse family;
    private AddressResponse address;
    private EmploymentResponse employment;
    private List<PreviousEmploymentResponse> previousEmployment;
    private IncomeResponse income;
    private BankResponse bank;
    private List<TaxReturnResponse> taxReturns;
    private List<LoanHistoryResponse> loanHistory;
    private List<ReferenceResponse> references;
}
