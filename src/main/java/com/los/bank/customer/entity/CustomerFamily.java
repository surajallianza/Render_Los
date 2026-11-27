package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer family details entity mapped to table c006.
 */
@Entity
@Table(name = "c006")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFamily {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "father_title_lookup_id")
    private Long fatherTitleLookupId;

    @Column(name = "father_first_name", length = 80)
    private String fatherFirstName;

    @Column(name = "father_middle_name", length = 80)
    private String fatherMiddleName;

    @Column(name = "father_last_name", length = 80)
    private String fatherLastName;

    @Column(name = "mother_title_lookup_id")
    private Long motherTitleLookupId;

    @Column(name = "mother_first_name", length = 80)
    private String motherFirstName;

    @Column(name = "mother_middle_name", length = 80)
    private String motherMiddleName;

    @Column(name = "mother_last_name", length = 80)
    private String motherLastName;

    @Column(name = "spouse_title_lookup_id")
    private Long spouseTitleLookupId;

    @Column(name = "spouse_first_name", length = 80)
    private String spouseFirstName;

    @Column(name = "spouse_middle_name", length = 80)
    private String spouseMiddleName;

    @Column(name = "spouse_last_name", length = 80)
    private String spouseLastName;
}
