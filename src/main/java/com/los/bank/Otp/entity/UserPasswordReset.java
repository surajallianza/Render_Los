package com.los.bank.Otp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping table 'u006' - Secure tracking for Password Reset requests.
 */
@Entity
@Table(name = "u006")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "new_password_hash", nullable = false, length = 255)
    private String newPasswordHash;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;

    @Builder.Default
    @Column(name = "applied", nullable = false)
    private Boolean applied = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
