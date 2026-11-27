package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer contact entity mapped to table c002.
 */
@Entity
@Table(name = "c002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerContact {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "first_name", length = 80, nullable = false)
    private String firstName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;
}
