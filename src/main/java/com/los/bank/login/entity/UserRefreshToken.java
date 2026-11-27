package com.los.bank.login.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping table 'u007' - Stored Refresh Token hashes.
 */
@Entity
@Table(name = "u007")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Builder.Default
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
