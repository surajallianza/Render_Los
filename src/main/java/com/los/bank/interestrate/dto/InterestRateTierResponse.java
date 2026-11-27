package com.los.bank.interestrate.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateTierResponse {
    private Long id;
    private Integer srNo;
    private BigDecimal upToAmount;
    private Integer upToMonths;
    private BigDecimal normalRate;
    private BigDecimal penalRate;
    private BigDecimal adhocRate;
}
