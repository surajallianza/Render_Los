package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanHistoryResponse {

    private Long id;
    private String bankName;
    private String accountNumber;
    private String loanType;
    private BigDecimal totalOutstanding;
    private BigDecimal emi;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private String source;
}
