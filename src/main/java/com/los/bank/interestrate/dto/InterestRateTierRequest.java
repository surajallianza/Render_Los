package com.los.bank.interestrate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.math.BigDecimal;

/**
 * One tier in a create/update request. snake_case names (as sent by the current
 * Interest Rate Master screen) are accepted too.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateTierRequest {

    @JsonAlias({"up_to_amount"})
    private BigDecimal upToAmount;

    @JsonAlias({"up_to_months"})
    private Integer upToMonths;

    @JsonAlias({"normal_rate", "normal_interest_rate", "normalInterest", "normalInterestRate"})
    private BigDecimal normalRate;

    @JsonAlias({"penal_rate"})
    private BigDecimal penalRate;

    @JsonAlias({"adhoc_rate"})
    private BigDecimal adhocRate;
}
