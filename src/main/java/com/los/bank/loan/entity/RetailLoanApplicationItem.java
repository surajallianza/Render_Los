package com.los.bank.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Child entity mapped to table a005 (Application Income / Expense Line Items).
 */
@Entity
@Table(name = "a005")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetailLoanApplicationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private RetailLoanApplication loanApplication;

    @Column(name = "item_type", length = 20)
    private String itemType;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "monthly_amount", precision = 15, scale = 2)
    private BigDecimal monthlyAmount;
}
