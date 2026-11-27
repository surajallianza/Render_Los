package com.los.bank.setup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_setup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false, unique = true)
    private Long bankId;

    @Column(name = "setup_completed", nullable = false)
    private Boolean setupCompleted;

    @Column(name = "database_created", nullable = false)
    private Boolean databaseCreated;

    @Column(name = "migration_completed", nullable = false)
    private Boolean migrationCompleted;

    @Column(name = "super_admin_created", nullable = false)
    private Boolean superAdminCreated;

    @Column(name = "setup_started_at")
    private LocalDateTime setupStartedAt;

    @Column(name = "setup_completed_at")
    private LocalDateTime setupCompletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (setupCompleted == null) setupCompleted = false;
        if (databaseCreated == null) databaseCreated = false;
        if (migrationCompleted == null) migrationCompleted = false;
        if (superAdminCreated == null) superAdminCreated = false;
    }
}

