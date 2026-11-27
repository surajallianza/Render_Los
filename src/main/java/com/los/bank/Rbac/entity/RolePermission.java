package com.los.bank.Rbac.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entity mapping table 'r003' - Role to Permission mappings.
 */
@Entity
@Table(name = "r003")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;
}
