package com.los.bank.branch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming Request DTO for creating or updating a Bank Branch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchRequestDto {

    private Long id;
    private String bankName;
    private String branchCode;
    private String branchName;
    private String rbiLicenseNo;
    private String registrationNo;
    private String incorporationDate;
    private String workingFrom;
    private String workingTo;
    private String address;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private String primaryContactNumber;
    private String secondaryContactNumber;
    private String emailId;
}
