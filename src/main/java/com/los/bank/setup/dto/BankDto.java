package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankDto {

    private String name;

    @JsonProperty("bank_code")
    private String bankCode;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("legal_name")
    private String legalName;

    @JsonProperty("bank_type")
    private String bankType;

    @JsonProperty("contact_email")
    @JsonAlias({"bank_email", "email"})
    private String contactEmail;

    @JsonProperty("contact_phone")
    @JsonAlias({"contact_number", "phone"})
    private String contactPhone;

    @JsonProperty("pan")
    @JsonAlias({"PAN"})
    private String pan;

    @JsonProperty("gst_no")
    @JsonAlias({"gst_number"})
    private String gstNo;

    @JsonProperty("license_no")
    @JsonAlias({"license_number"})
    private String licenseNo;

    private String website;

    @JsonProperty("logo_url")
    @JsonAlias({"logo"})
    private String logoUrl;

    private String status;

    @JsonProperty("regulatory_status")
    private String regulatoryStatus;

    @JsonProperty("direct_clg_member")
    @JsonAlias({"direct_clearing_member", "direct_clearing_number"})
    private Boolean directClgMember;

    @JsonProperty("direct_member_iftas")
    private Boolean directMemberIftas;

    private String micr1;
    private String micr2;
    private String micr3;

    @JsonProperty("micr_number")
    @JsonAlias({"micr_details"})
    private String micrNumber;

    @JsonProperty("ifsc_code")
    private String ifscCode;

    @JsonProperty("no_of_branches")
    @JsonAlias({"number_of_branches"})
    private Integer noOfBranches;

    @JsonProperty("branch_name")
    @JsonAlias({"main_branch_name"})
    private String branchName;

    @JsonProperty("branch_code")
    @JsonAlias({"main_branch_code"})
    private String branchCode;

    @JsonProperty("sponsor_bank_clg")
    @JsonAlias({"sponsor_bank_of_clearing"})
    private String sponsorBankClg;

    @JsonProperty("sponsor_bank_iftas")
    @JsonAlias({"sponsor_bank_of_iftas"})
    private String sponsorBankIftas;

    @JsonProperty("address_type")
    private String addressType;

    @JsonProperty("unit_gala")
    @JsonAlias({"unit_gala_name_and_number"})
    private String unitGala;

    @JsonProperty("street_road")
    private String streetRoad;

    private String landmark;
    private String city;
    private String state;
    private String country;
    private String pincode;

    // Optional legacy address string
    private String address;

    public String getEffectiveBankName() {
        if (bankName != null && !bankName.trim().isEmpty()) {
            return bankName.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (legalName != null && !legalName.trim().isEmpty()) {
            return legalName.trim();
        }
        return "Bank";
    }

    public String getEffectiveBankCode() {
        if (bankCode != null && !bankCode.trim().isEmpty()) {
            return bankCode.trim().toUpperCase();
        }
        return null;
    }

    public String getEffectiveEmail() {
        if (contactEmail != null && !contactEmail.trim().isEmpty()) {
            return contactEmail.trim();
        }
        return "info@" + (bankCode != null ? bankCode.trim().toLowerCase() : "bank") + ".com";
    }

    public String getEffectiveContactNumber() {
        if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            return contactPhone.trim();
        }
        return "";
    }

    public String getEffectiveAddress() {
        if (address != null && !address.trim().isEmpty()) {
            return address.trim();
        }
        StringBuilder sb = new StringBuilder();
        if (unitGala != null && !unitGala.trim().isEmpty()) {
            sb.append(unitGala.trim());
        }
        if (streetRoad != null && !streetRoad.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(streetRoad.trim());
        }
        if (landmark != null && !landmark.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", Near ").append(landmark.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        if (state != null && !state.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state.trim());
        }
        if (country != null && !country.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country.trim());
        }
        if (pincode != null && !pincode.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(pincode.trim());
        }
        return sb.toString();
    }
}
