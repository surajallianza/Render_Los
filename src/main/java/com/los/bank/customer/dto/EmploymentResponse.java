package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmploymentResponse {

    private LookupValue employmentType;
    private LookupValue employerType;
    private String designation;
    private String employerName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfJoining;

    private String qualification;
    private String employerEmail;
    private LookupValue listedStatus;
    private BigDecimal yearsOfService;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfRetirement;
}
