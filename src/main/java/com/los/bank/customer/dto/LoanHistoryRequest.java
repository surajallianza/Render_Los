package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanHistoryRequest {

    private String bankName;
    private String accountNumber;
    private String loanType;
    private BigDecimal totalOutstanding;
    private BigDecimal emi;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
}
