package com.los.bank.Rbac.service;

import com.los.bank.Rbac.dto.RolePermissionAssignRequest;
import com.los.bank.Rbac.dto.RolePermissionAssignResponse;
import com.los.bank.Rbac.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RbacService {

    /**
     * Dynamically fetches role from r001 table by role id.
     */
    Optional<Role> getRoleById(Integer roleId);

    /**
     * Dynamically fetches role from r001 table by role name (case-insensitive).
     */
    Optional<Role> getRoleByName(String name);

    /**
     * Retrieves all permission codes assigned to a role via r003 and r002.
     */
    List<String> getPermissionsForRole(Integer roleId);

    /**
     * Checks if a role has the specified permission code mapped via r003 and r002.
     */
    boolean hasPermission(Integer roleId, String permissionCode);

    /**
     * Assigns and synchronizes permissions for an existing role in r003 table.
     * Validates role against r001 and permissions against r002.
     * Retains active mappings, deletes deselected mappings, and inserts newly selected mappings.
     *
     * @param request the assignment payload
     * @return the assignment response summary
     */
    RolePermissionAssignResponse assignRolePermissions(RolePermissionAssignRequest request);

    /**
     * Updates and synchronizes permissions for an existing role in r003 table.
     * Validates role against r001 and permissions against r002.
     * Retains active mappings, deletes deselected mappings, and inserts newly selected mappings.
     *
     * @param request the update payload
     * @return the update response summary
     */
    RolePermissionAssignResponse updateRolePermissions(RolePermissionAssignRequest request);
}
