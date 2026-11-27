package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an organization / bank account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationAccountDto {

    private String particulars;

    @JsonProperty("bankName")
    @JsonAlias({"bank_name"})
    private String bankName;

    @JsonProperty("accountType")
    @JsonAlias({"account_type"})
    private String accountType;

    @JsonProperty("accountNumber")
    @JsonAlias({"account_number"})
    private String accountNumber;

    @JsonProperty("micrCode")
    @JsonAlias({"micr_code", "micr"})
    private String micrCode;

    @JsonProperty("IFSCCode")
    @JsonAlias({"ifscCode", "ifsc_code", "ifsc"})
    private String ifscCode;
}
