package com.los.bank.interestrate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One rate tier of a rate sheet (table i002): loans up to {@link #upToAmount}
 * and up to {@link #upToMonths} get {@link #normalRate}.
 */
@Entity
@Table(name = "i002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_master_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private InterestRateMaster rateMaster;

    @Column(name = "sr_no", nullable = false)
    private Integer srNo;

    @Column(name = "up_to_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal upToAmount;

    @Column(name = "up_to_months", nullable = false)
    private Integer upToMonths;

    @Column(name = "normal_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal normalRate;

    @Column(name = "penal_rate", precision = 6, scale = 2)
    private BigDecimal penalRate;

    @Column(name = "adhoc_rate", precision = 6, scale = 2)
    private BigDecimal adhocRate;
}
