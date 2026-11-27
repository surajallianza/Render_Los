package com.los.bank.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request payload model for creating or updating a Loan Product.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanProductRequestDto {

    @JsonProperty("product_code")
    @JsonAlias({"productCode", "code"})
    private String productCode;

    @JsonProperty("product_name")
    @JsonAlias({"productName", "name"})
    private String productName;

    private String description;

    private String status;

    private String emi;

    @JsonProperty("emi_type")
    @JsonAlias({"emiType"})
    private String emiType;

    @JsonProperty("emi_fixed_date")
    @JsonAlias({"emiFixedDate"})
    private String emiFixedDate;

    public void setEmiFixedDate(Object val) {
        if (val == null) {
            this.emiFixedDate = null;
        } else if (val instanceof Boolean) {
            this.emiFixedDate = ((Boolean) val) ? "Yes" : "No";
        } else {
            this.emiFixedDate = val.toString().trim();
        }
    }

    @JsonProperty("min_tenure_months")
    @JsonAlias({"minTenureMonths"})
    private Integer minTenureMonths;

    @JsonProperty("max_tenure_months")
    @JsonAlias({"maxTenureMonths"})
    private Integer maxTenureMonths;

    @JsonProperty("min_amount")
    @JsonAlias({"minAmount"})
    private BigDecimal minAmount;

    @JsonProperty("max_amount")
    @JsonAlias({"maxAmount"})
    private BigDecimal maxAmount;

    @JsonProperty("int_base_method")
    @JsonAlias({"intBaseMethod"})
    private String intBaseMethod;

    @JsonProperty("no_of_days_in_year")
    @JsonAlias({"noOfDaysInYear"})
    private Integer noOfDaysInYear;

    @JsonProperty("interest_calculation_frequency")
    @JsonAlias({"interestCalculationFrequency"})
    private String interestCalculationFrequency;

    @JsonProperty("interest_calculation_mode")
    @JsonAlias({"interestCalculationMode"})
    private String interestCalculationMode;

    private Boolean penalty;

    @JsonProperty("penalty_percent")
    @JsonAlias({"penaltyPercent"})
    private BigDecimal penaltyPercent;

    @JsonProperty("delegation_of_power")
    @JsonAlias({"delegationOfPower", "delegations"})
    @Builder.Default
    private List<DelegationOfPowerDto> delegationOfPower = new ArrayList<>();
}
