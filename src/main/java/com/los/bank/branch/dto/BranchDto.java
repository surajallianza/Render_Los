package com.los.bank.branch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing detailed branch information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BranchDto {

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
