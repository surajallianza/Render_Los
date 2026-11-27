package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FamilyResponse {

    private LookupValue fatherTitle;
    private String fatherFirstName;
    private String fatherMiddleName;
    private String fatherLastName;

    private LookupValue motherTitle;
    private String motherFirstName;
    private String motherMiddleName;
    private String motherLastName;

    private LookupValue spouseTitle;
    private String spouseFirstName;
    private String spouseMiddleName;
    private String spouseLastName;
}
