package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerListItemDto {

    private String customerCode;
    private String fullName;
    private String customerType;
    private String kycStatus;
    private String mobile;
    private String email;
    private String branch;
    private Boolean isActive;
    private String onboardingStatus;
    private LocalDate dob;
    private Integer age;
    private LocalDateTime createdAt;
}
