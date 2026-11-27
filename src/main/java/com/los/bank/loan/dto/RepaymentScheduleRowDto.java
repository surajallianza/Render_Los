package com.los.bank.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single row of the generated EMI repayment schedule chart.
 * Preserves the exact external JSON field names.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepaymentScheduleRowDto {

    @JsonProperty("installmentNumber")
    private Integer installmentNumber;

    @JsonProperty("Month")
    private String month;

    @JsonProperty("Principal Amount (Rs.)")
    private String principalAmount;

    @JsonProperty("Installment Amount (Rs.)")
    private String installmentAmount;

    @JsonProperty("Interest (Rs.)")
    private String interest;

    @JsonProperty("Principal (Rs.)")
    private String principal;

    @JsonProperty("Balance Amount (Rs.)")
    private String balanceAmount;
}
