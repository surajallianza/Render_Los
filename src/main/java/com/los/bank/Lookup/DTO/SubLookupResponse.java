package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubLookupResponse {

    private Long id;

    @JsonProperty("lookupTypeCode")
    private String lookupTypeCode;

    @JsonProperty("sub_lookup_code")
    private String subLookupCode;

    private String code;
    private String name;
    private String description;
    private String status;

    @JsonProperty("displayOrder")
    private Integer displayOrder;

    @JsonProperty("isFixed")
    private boolean isFixed;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
