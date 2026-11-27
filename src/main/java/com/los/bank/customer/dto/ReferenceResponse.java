package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferenceResponse {

    private Long id;
    private Integer srNo;
    private String referenceName;
    private String relation;
    private String location;
    private String contactNo;
    private String email;
}
