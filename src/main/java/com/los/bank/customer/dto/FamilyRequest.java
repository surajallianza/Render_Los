package com.los.bank.customer.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyRequest {

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String fatherTitle;
    private String fatherFirstName;
    private String fatherMiddleName;
    private String fatherLastName;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String motherTitle;
    private String motherFirstName;
    private String motherMiddleName;
    private String motherLastName;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String spouseTitle;
    private String spouseFirstName;
    private String spouseMiddleName;
    private String spouseLastName;
}
