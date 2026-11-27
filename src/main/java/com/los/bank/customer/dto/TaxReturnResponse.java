package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxReturnResponse {

    private Long id;
    private String financialYear;
    private BigDecimal grossIncome;
    private BigDecimal tdsPaid;
}
