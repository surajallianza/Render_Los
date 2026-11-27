package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Customer income details entity mapped to table c010.
 */
@Entity
@Table(name = "c010")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerIncome {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "other_source_income", precision = 15, scale = 2)
    private BigDecimal otherSourceIncome;

    @Column(name = "total_gross_income", precision = 15, scale = 2)
    private BigDecimal totalGrossIncome;

    @Column(name = "total_net_income", precision = 15, scale = 2)
    private BigDecimal totalNetIncome;

    @Column(name = "income_tax", precision = 15, scale = 2)
    private BigDecimal incomeTax;

    @Column(name = "income_tax_filer_status_lookup_id")
    private Long incomeTaxFilerStatusLookupId;

    @Column(name = "risk_category_lookup_id")
    private Long riskCategoryLookupId;
}
