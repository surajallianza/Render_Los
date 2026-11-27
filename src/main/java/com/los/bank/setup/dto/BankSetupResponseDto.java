package com.los.bank.setup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankSetupResponseDto {
    private boolean success;
    private String message;
    private BankSetupDataDto data;
}

