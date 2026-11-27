package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing complete Organization / Bank Master details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationDto {

    private Long id;

    @JsonProperty("bankCode")
    @JsonAlias({"bank_code"})
    private String bankCode;

    @JsonProperty("bankName")
    @JsonAlias({"bank_name", "name"})
    private String bankName;

    @JsonProperty("bankType")
    @JsonAlias({"bank_type"})
    private String bankType;

    @JsonProperty("RegistrationlicenseNumber")
    @JsonAlias({"registrationLicenseNumber", "license_no", "licenseNumber", "registration_license_number"})
    private String registrationLicenseNumber;

    @JsonProperty("pan")
    @JsonAlias({"PAN"})
    private String pan;

    @JsonProperty("gstNumber")
    @JsonAlias({"gst_number", "gstNo", "gst_no"})
    private String gstNumber;

    @JsonProperty("No.ofBranches")
    @JsonAlias({"noOfBranches", "no_of_branches", "numberOfBranches"})
    private String numberOfBranches;

    @JsonProperty("addressType")
    @JsonAlias({"address_type"})
    private String addressType;

    @JsonProperty("address")
    private String address;

    @JsonProperty("addressLine2")
    @JsonAlias({"address_line_2", "address_line2", "street_road", "streetRoad"})
    private String addressLine2;

    @JsonProperty("landmark")
    private String landmark;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("pincode")
    @JsonAlias({"pinCode", "pin_code"})
    private String pincode;

    @JsonProperty("singlePartyLimit")
    @JsonAlias({"single_party_limit"})
    private String singlePartyLimit;

    @JsonProperty("groupExposureLimit")
    @JsonAlias({"group_exposure_limit"})
    private String groupExposureLimit;

    @JsonProperty("sanctionMargin")
    @JsonAlias({"sanction_margin"})
    private String sanctionMargin;

    @JsonProperty("penaltyCharge")
    @JsonAlias({"penalty_charge"})
    private String penaltyCharge;

    @JsonProperty("penaltyRate")
    @JsonAlias({"penalty_rate"})
    private String penaltyRate;

    @JsonProperty("LoanApprovalAuthority")
    @JsonAlias({"loanApprovalAuthority", "loan_approval_authority"})
    private String loanApprovalAuthority;

    @JsonProperty("accounts")
    @Builder.Default
    private List<OrganizationAccountDto> accounts = new ArrayList<>();
}
