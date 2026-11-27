package com.los.bank.customer.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeRequest {

    private BigDecimal annualIncome;
    private BigDecimal otherSourceIncome;
    private BigDecimal totalGrossIncome;
    private BigDecimal totalNetIncome;
    private BigDecimal incomeTax;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String incomeTaxFilerStatus;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String riskCategory;
}
