package com.los.bank.Rbac.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping table 'r002' - Granular Permission codes.
 */
@Entity
@Table(name = "r002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "module", length = 60)
    private String module;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
