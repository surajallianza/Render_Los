package com.los.bank.setup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "initial_admin_setup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitialAdminSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false, unique = true)
    private Long bankId;

    @Column(name = "admin_name", nullable = false, length = 150)
    private String adminName;

    @Column(name = "admin_email", nullable = false, length = 150)
    private String adminEmail;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "is_created", nullable = false)
    private Boolean isCreated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isCreated == null) {
            isCreated = false;
        }
    }
}

