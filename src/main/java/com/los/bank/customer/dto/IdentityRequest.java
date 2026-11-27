package com.los.bank.customer.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityRequest {

    private String panNumber;
    private String panValidationStatus;
    private String aadhaarNumber;
    private String aadhaarValidationStatus;
    private String ckycNumber;
}
