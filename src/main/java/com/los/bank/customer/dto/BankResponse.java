package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankResponse {

    private String bankName;
    private String accountNumber;
    private LookupValue accountType;
    private LookupValue accountStatus;
}
