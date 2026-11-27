package com.los.bank.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing lead address details.
 * Maps to bank database table l006.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressDetailDto {

    private Long id;

    @JsonProperty("addressType")
    @JsonAlias({"address_type", "particulars"})
    private String addressType;

    @JsonProperty("flatBuildingName")
    @JsonAlias({"flat_building_name", "house_flat_no", "houseFlatNo"})
    private String flatBuildingName;

    @JsonProperty("streetRoad")
    @JsonAlias({"street_road", "street_address", "streetAddress"})
    private String streetRoad;

    @JsonProperty("landmark")
    @JsonAlias({"locality"})
    private String landmark;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("pinCode")
    @JsonAlias({"pin_code", "pincode"})
    private String pinCode;
}
