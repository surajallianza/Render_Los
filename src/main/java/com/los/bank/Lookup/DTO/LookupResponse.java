package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;

    @JsonProperty("isFixed")
    private boolean isFixed;

    @JsonProperty("subLookupCount")
    private int subLookupCount;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("subLookups")
    private List<SubLookupResponse> subLookups;
}
