package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer identity details entity mapped to table c005.
 * Sensitive columns are encrypted BYTEA, with keyed HMAC hashes for deduplication.
 */
@Entity
@Table(name = "c005")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerIdentity {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pan_number", columnDefinition = "BYTEA")
    private byte[] panNumber;

    @Column(name = "pan_hash", length = 64)
    private String panHash;

    @Column(name = "pan_validation_status", length = 50)
    private String panValidationStatus;

    @Column(name = "aadhaar_number", columnDefinition = "BYTEA")
    private byte[] aadhaarNumber;

    @Column(name = "aadhaar_hash", length = 64)
    private String aadhaarHash;

    @Column(name = "aadhaar_validation_status", length = 50)
    private String aadhaarValidationStatus;

    @Column(name = "ckyc_number", columnDefinition = "BYTEA")
    private byte[] ckycNumber;
}
