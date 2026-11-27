package com.los.bank.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete API response model for a Loan Product including nested delegation details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanProductResponseDto {

    private Long id;

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("product_name")
    private String productName;

    private String description;

    private String status;

    private String emi;

    @JsonProperty("emi_type")
    private String emiType;

    @JsonProperty("emi_fixed_date")
    private String emiFixedDate;

    @JsonProperty("min_tenure_months")
    private Integer minTenureMonths;

    @JsonProperty("max_tenure_months")
    private Integer maxTenureMonths;

    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

    @JsonProperty("int_base_method")
    private String intBaseMethod;

    @JsonProperty("no_of_days_in_year")
    private Integer noOfDaysInYear;

    @JsonProperty("interest_calculation_frequency")
    private String interestCalculationFrequency;

    @JsonProperty("interest_calculation_mode")
    private String interestCalculationMode;

    private Boolean penalty;

    @JsonProperty("penalty_percent")
    private BigDecimal penaltyPercent;

    @JsonProperty("delegation_of_power")
    @Builder.Default
    private List<DelegationOfPowerDto> delegationOfPower = new ArrayList<>();

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
