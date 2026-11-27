package com.los.bank.customer.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxReturnRequest {

    private String financialYear;
    private BigDecimal grossIncome;
    private BigDecimal tdsPaid;
}
