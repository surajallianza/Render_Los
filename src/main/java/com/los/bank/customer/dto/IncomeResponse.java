package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncomeResponse {

    private BigDecimal annualIncome;
    private BigDecimal otherSourceIncome;
    private BigDecimal totalGrossIncome;
    private BigDecimal totalNetIncome;
    private BigDecimal incomeTax;
    private LookupValue incomeTaxFilerStatus;
    private LookupValue riskCategory;
}
