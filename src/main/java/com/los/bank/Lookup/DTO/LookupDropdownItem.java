package com.los.bank.lookup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupDropdownItem {
    private String code;

    @JsonProperty("sub_lookup_code")
    private String subLookupCode;

    private String name;
    private Integer displayOrder;
}
