package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer bank account entity mapped to table c011.
 * account_number is encrypted BYTEA.
 */
@Entity
@Table(name = "c011")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerBankAccount {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_number", columnDefinition = "BYTEA")
    private byte[] accountNumber;

    @Column(name = "account_type_lookup_id")
    private Long accountTypeLookupId;

    @Column(name = "account_status_lookup_id")
    private Long accountStatusLookupId;
}
