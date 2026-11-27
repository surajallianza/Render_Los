package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressResponse {

    private LookupValue residentialStatus;
    private LookupValue residentialType;
    private String flatBuildingName;
    private String streetRoad;
    private String landmark;
    private String city;
    private LookupValue state;
    private String pincode;
}
