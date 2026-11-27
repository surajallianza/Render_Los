package com.los.bank.goldrate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Gold Rate Master activity record with complete audit trail details.
 * Matches exact casing and structure of the reference JSON payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class GoldRateActivityDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("date")
    private String date;

    @JsonProperty("time")
    private String time;

    @JsonProperty("module")
    private String module;

    @JsonProperty("recordId")
    private String recordId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("field")
    private String field;

    @JsonProperty("oldValue")
    private String oldValue;

    @JsonProperty("newValue")
    private String newValue;

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

    @JsonProperty("status")
    private String status;

    @JsonProperty("description")
    private String description;

    @JsonProperty("ip")
    private String ip;
}
