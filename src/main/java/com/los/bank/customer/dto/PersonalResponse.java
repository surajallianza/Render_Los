package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonalResponse {

    private LookupValue title;
    private String firstName;
    private String middleName;
    private String lastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private Integer age;

    private LookupValue gender;
    private LookupValue maritalStatus;
    private LookupValue customerType;
    private LookupValue kycStatus;

    private String occupation;
    private String mobile;
    private String alternateContact;
    private String email;
    private String leadId;
    private Long branchId;
}
