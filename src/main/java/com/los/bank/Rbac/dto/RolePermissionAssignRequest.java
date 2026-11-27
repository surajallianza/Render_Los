package com.los.bank.Rbac.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionAssignRequest {

    @NotNull(message = "roleId is required")
    private Integer roleId;

    private String roleName;

    private String description;

    @NotNull(message = "totalSelected is required")
    private Integer totalSelected;

    private List<Integer> permissionIds;

    private List<String> permissionCodes;
}
