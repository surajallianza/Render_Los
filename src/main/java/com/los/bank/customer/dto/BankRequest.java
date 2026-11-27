package com.los.bank.customer.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankRequest {

    private String bankName;
    private String accountNumber;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String accountType;

    @JsonDeserialize(using = LookupCodeDeserializer.class)
    private String accountStatus;
}
