package com.los.bank.Otp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping table 'u005' - Secure storage of OTP hashes for 2FA and sensitive operations.
 */
@Entity
@Table(name = "u005")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Builder.Default
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
