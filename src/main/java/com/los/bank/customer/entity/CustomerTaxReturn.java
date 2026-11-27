package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Customer income tax returns entity mapped to table c012.
 */
@Entity
@Table(name = "c012", uniqueConstraints = {
        @UniqueConstraint(name = "uq_c012_customer_year", columnNames = {"customer_id", "financial_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTaxReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "financial_year", length = 7, nullable = false)
    private String financialYear;

    @Column(name = "gross_income", precision = 15, scale = 2)
    private BigDecimal grossIncome;

    @Column(name = "tds_paid", precision = 15, scale = 2)
    private BigDecimal tdsPaid;
}
