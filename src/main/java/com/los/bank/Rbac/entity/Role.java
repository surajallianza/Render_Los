package com.los.bank.Rbac.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping table 'r001' - Dynamic Application Roles (Maker, Checker, Viewer, Admin, Super Admin).
 */
@Entity
@Table(name = "r001")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "panel", length = 30)
    private String panel;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
