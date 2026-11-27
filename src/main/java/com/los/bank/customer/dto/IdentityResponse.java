package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityResponse {

    private String panNumber;
    private String panValidationStatus;
    private String aadhaarNumber;
    private String aadhaarValidationStatus;
    private String ckycNumber;
}
