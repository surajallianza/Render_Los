package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRequest {

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String title;

    private String firstName;
    private String middleName;
    private String lastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String gender;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String maritalStatus;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String customerType;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String kycStatus;

    private String occupation;
    private String mobile;
    private String alternateContact;
    private String email;
    private String leadId;
    private Long branchId;
}
