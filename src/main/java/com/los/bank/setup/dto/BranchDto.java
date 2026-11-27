package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for branch information mapped to bank database table b001.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchDto {

    private Long id;

    @JsonProperty("branch_name")
    @JsonAlias({"name", "branchName"})
    private String branchName;

    @JsonProperty("branch_code")
    @JsonAlias({"code", "branchCode"})
    private String branchCode;

    private String address;
    private String city;
    private String state;

    @JsonProperty("pincode")
    @JsonAlias({"pin_code", "pinCode", "postal_code", "zip"})
    private String pincode;

    private String status;

    public String getEffectiveBranchName(String defaultName) {
        if (branchName != null && !branchName.trim().isEmpty()) {
            return branchName.trim();
        }
        return defaultName;
    }

    public String getEffectiveBranchCode(String defaultCode) {
        if (branchCode != null && !branchCode.trim().isEmpty()) {
            return branchCode.trim().toUpperCase();
        }
        return defaultCode;
    }
}
