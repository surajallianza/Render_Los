package com.los.bank.login.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping table 'u008' - JWT token tracking and session invalidation by JTI.
 */
@Entity
@Table(name = "u008")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJwtSession {

    @Id
    @Column(name = "jti", nullable = false, length = 100)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "timeout_secs")
    private Integer timeoutSecs;

    @Builder.Default
    @Column(name = "invalidated", nullable = false)
    private Boolean invalidated = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
