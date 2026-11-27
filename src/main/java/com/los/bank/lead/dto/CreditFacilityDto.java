package com.los.bank.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an existing or other bank credit facility.
 * Maps to bank database tables l007_existing_facilities and l008_other_facilities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditFacilityDto {

    private Long id;

    @JsonProperty("bankName")
    @JsonAlias({"bank_name"})
    private String bankName;

    @JsonProperty("accountNumber")
    @JsonAlias({"account_number"})
    private String accountNumber;

    @JsonProperty("loanType")
    @JsonAlias({"loan_type"})
    private String loanType;

    @JsonProperty("limit")
    @JsonAlias({"credit_limit", "creditLimit"})
    private String limit;

    @JsonProperty("emi")
    private String emi;

    @JsonProperty("outstanding")
    private String outstanding;

    @JsonProperty("dpd")
    private String dpd;

    @JsonProperty("accountStatus")
    @JsonAlias({"account_status"})
    private String accountStatus;

    @JsonProperty("statementRequested")
    @JsonAlias({"statement_requested"})
    private Boolean statementRequested;
}
