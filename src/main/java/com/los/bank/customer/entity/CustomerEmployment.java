package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer employment details entity mapped to table c008.
 */
@Entity
@Table(name = "c008")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEmployment {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "employment_type_lookup_id")
    private Long employmentTypeLookupId;

    @Column(name = "employer_type_lookup_id")
    private Long employerTypeLookupId;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "employer_name", length = 150)
    private String employerName;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "qualification", length = 100)
    private String qualification;

    @Column(name = "employer_email", length = 150)
    private String employerEmail;

    @Column(name = "listed_status_lookup_id")
    private Long listedStatusLookupId;

    @Column(name = "years_of_service", precision = 5, scale = 1)
    private BigDecimal yearsOfService;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "date_of_retirement")
    private LocalDate dateOfRetirement;
}
