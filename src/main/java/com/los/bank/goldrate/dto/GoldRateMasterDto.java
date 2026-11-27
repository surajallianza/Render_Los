package com.los.bank.goldrate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a Gold Rate Master business record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoldRateMasterDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("recordId")
    private String recordId;

    @JsonProperty("effectiveDate")
    private String effectiveDate;

    @JsonProperty("effectiveTime")
    private String effectiveTime;

    @JsonProperty("rate24k")
    private BigDecimal rate24k;

    @JsonProperty("rate22k")
    private BigDecimal rate22k;

    @JsonProperty("rate18k")
    private BigDecimal rate18k;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("status")
    private String status;

    @JsonProperty("description")
    private String description;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdById")
    private String createdById;

    @JsonProperty("modifiedBy")
    private String modifiedBy;

    @JsonProperty("modifiedById")
    private String modifiedById;

    @JsonProperty("verifiedBy")
    private String verifiedBy;

    @JsonProperty("verifiedById")
    private String verifiedById;
}
