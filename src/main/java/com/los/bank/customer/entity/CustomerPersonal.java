package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Customer personal details entity mapped to table c004.
 * Note: c004 has NO created_at or updated_at; c001.updated_at is updated instead.
 */
@Entity
@Table(name = "c004")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPersonal {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "lead_id", length = 50, unique = true)
    private String leadId;

    @Column(name = "title_lookup_id")
    private Long titleLookupId;

    @Column(name = "gender_lookup_id")
    private Long genderLookupId;

    @Column(name = "marital_status_lookup_id")
    private Long maritalStatusLookupId;

    @Column(name = "customer_type_lookup_id")
    private Long customerTypeLookupId;

    @Column(name = "kyc_status_lookup_id")
    private Long kycStatusLookupId;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "occupation", length = 50)
    private String occupation;

    @Column(name = "alternate_contact", length = 10)
    private String alternateContact;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "modified_by")
    private Long modifiedBy;
}
