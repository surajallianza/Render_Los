package com.los.bank.customer.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceRequest {

    private Integer srNo;
    private String referenceName;
    private String relation;
    private String location;
    private String contactNo;
    private String email;
}
