package com.los.bank.setup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "database_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabaseConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false, unique = true)
    private Long bankId;

    @Column(name = "database_name", nullable = false, unique = true, length = 100)
    private String databaseName;

    @Column(name = "database_host", nullable = false, length = 255)
    private String databaseHost;

    @Column(name = "database_port", nullable = false)
    private Integer databasePort;

    @Column(name = "database_type", length = 50)
    private String databaseType;

    @Column(name = "is_connected")
    private Boolean isConnected;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (databasePort == null) {
            databasePort = 5432;
        }
        if (databaseType == null) {
            databaseType = "POSTGRESQL";
        }
        if (isConnected == null) {
            isConnected = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

