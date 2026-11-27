package com.los.bank.Rbac.controller;

import com.los.bank.Rbac.dto.RolePermissionAssignRequest;
import com.los.bank.Rbac.dto.RolePermissionAssignResponse;
import com.los.bank.Rbac.entity.Permission;
import com.los.bank.Rbac.entity.Role;
import com.los.bank.Rbac.exception.RbacValidationException;
import com.los.bank.Rbac.repository.PermissionRepository;
import com.los.bank.Rbac.repository.RoleRepository;
import com.los.bank.Rbac.service.RbacService;
import com.los.bank.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    private final RbacService rbacService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacController(RbacService rbacService,
                          RoleRepository roleRepository,
                          PermissionRepository permissionRepository) {
        this.rbacService = rbacService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Retrieves all dynamic roles configured in r001 table.
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Roles fetched successfully", roles));
    }

    /**
     * Retrieves all available permissions in r002 table.
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Permissions fetched successfully", permissions));
    }

    /**
     * Retrieves all dynamic permissions mapped to a specific role.
     */
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getPermissionsForRole(@PathVariable Integer roleId) {
        List<String> permissions = rbacService.getPermissionsForRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Permissions fetched successfully", permissions));
    }

    /**
     * Assigns and synchronizes permissions for an existing role.
     * Endpoint: POST /api/rbac/roles/permissions
     */
    @PostMapping("/roles/permissions")
    public ResponseEntity<RolePermissionAssignResponse> assignPermissions(
            @Valid @RequestBody RolePermissionAssignRequest request) {
        RolePermissionAssignResponse response = rbacService.assignRolePermissions(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates/edits and synchronizes permissions for an existing role.
     * Endpoint: PUT /api/rbac/roles/permissions
     */
    @PutMapping({"/roles/permissions", "/roles/{roleId}/permissions"})
    public ResponseEntity<RolePermissionAssignResponse> updatePermissions(
            @PathVariable(required = false) Integer roleId,
            @Valid @RequestBody RolePermissionAssignRequest request) {
        if (roleId != null && request.getRoleId() == null) {
            request.setRoleId(roleId);
        }
        RolePermissionAssignResponse response = rbacService.updateRolePermissions(request);
        return ResponseEntity.ok(response);
    }
}
