package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Customer previous employment entity mapped to table c009.
 */
@Entity
@Table(name = "c009")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPreviousEmployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "employment_type_lookup_id")
    private Long employmentTypeLookupId;

    @Column(name = "employer_name", length = 150)
    private String employerName;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "date_of_relieving")
    private LocalDate dateOfRelieving;
}
