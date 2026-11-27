package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer manual loan history entity mapped to table c014.
 */
@Entity
@Table(name = "c014")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerManualLoanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "loan_type", length = 100)
    private String loanType;

    @Column(name = "total_outstanding", precision = 15, scale = 2)
    private BigDecimal totalOutstanding;

    @Column(name = "emi", precision = 15, scale = 2)
    private BigDecimal emi;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
