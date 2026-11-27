package com.los.bank.interestrate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Create / update request for a rate sheet. snake_case names (as sent by the current
 * Interest Rate Master screen: product_code, effective_date, tiers[].up_to_amount ...)
 * are accepted too. Product name is taken from the product, never from the request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateMasterRequest {

    @JsonAlias({"product_code"})
    private String productCode;

    @JsonAlias({"effective_date"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    private String remarks;

    @JsonAlias({"bands"})
    @Builder.Default
    private List<InterestRateTierRequest> tiers = new ArrayList<>();
}
