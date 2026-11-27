package com.los.bank.customer.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String residentialStatus;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String residentialType;

    private String flatBuildingName;
    private String streetRoad;
    private String landmark;
    private String city;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String state;

    private String pincode;
}
