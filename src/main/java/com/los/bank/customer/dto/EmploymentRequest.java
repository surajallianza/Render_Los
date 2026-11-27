package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentRequest {

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String employmentType;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String employerType;

    private String designation;
    private String employerName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfJoining;

    private String qualification;
    private String employerEmail;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String listedStatus;

    private BigDecimal yearsOfService;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfRetirement;
}
