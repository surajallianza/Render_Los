package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer reference entity mapped to table c015.
 * Limited to at most 2 per customer (sr_no 1 or 2).
 */
@Entity
@Table(name = "c015", uniqueConstraints = {
        @UniqueConstraint(name = "uq_c015_customer_sr_no", columnNames = {"customer_id", "sr_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "sr_no", nullable = false)
    private Integer srNo;

    @Column(name = "reference_name", length = 80, nullable = false)
    private String referenceName;

    @Column(name = "relation", length = 50)
    private String relation;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "contact_no", length = 10)
    private String contactNo;

    @Column(name = "email", length = 150)
    private String email;
}
