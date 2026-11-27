package com.los.bank.interestrate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rate a new loan gets. The loan should copy normalRate/penalRate and keep
 * rateMasterId + tierId, so later rate changes do not affect it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicableRateResponse {
    private String productCode;
    private String productName;
    private BigDecimal amount;
    private Integer tenureMonths;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate asOfDate;

    private Long rateMasterId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    private Long tierId;
    private Integer tierSrNo;
    private BigDecimal upToAmount;
    private Integer upToMonths;

    private BigDecimal normalRate;
    private BigDecimal penalRate;
    /** TIER when the tier has a penal rate, PRODUCT when the product penalty is used, null when none. */
    private String penalRateSource;
    private BigDecimal adhocRate;
}
