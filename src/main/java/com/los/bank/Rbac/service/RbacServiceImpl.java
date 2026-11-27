package com.los.bank.Rbac.service;

import com.los.bank.Rbac.dto.RolePermissionAssignRequest;
import com.los.bank.Rbac.dto.RolePermissionAssignResponse;
import com.los.bank.Rbac.entity.Permission;
import com.los.bank.Rbac.entity.Role;
import com.los.bank.Rbac.entity.RolePermission;
import com.los.bank.Rbac.entity.RolePermissionId;
import com.los.bank.Rbac.exception.RbacValidationException;
import com.los.bank.Rbac.exception.RoleNotFoundException;
import com.los.bank.Rbac.repository.PermissionRepository;
import com.los.bank.Rbac.repository.RolePermissionRepository;
import com.los.bank.Rbac.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RbacServiceImpl implements RbacService {

    private static final Logger logger = LoggerFactory.getLogger(RbacServiceImpl.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RbacServiceImpl(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional(value = "userTransactionManager", readOnly = true)
    public Optional<Role> getRoleById(Integer roleId) {
        if (roleId == null) {
            return Optional.empty();
        }
        return roleRepository.findById(roleId);
    }

    @Override
    @Transactional(value = "userTransactionManager", readOnly = true)
    public Optional<Role> getRoleByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return roleRepository.findByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional(value = "userTransactionManager", readOnly = true)
    public List<String> getPermissionsForRole(Integer roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }

        List<RolePermission> mappings = rolePermissionRepository.findByIdRoleId(roleId);
        if (mappings.isEmpty()) {
            logger.debug("[RbacService] No permissions mapped for Role ID: {}", roleId);
            return Collections.emptyList();
        }

        List<Integer> permissionIds = mappings.stream()
                .map(m -> m.getId().getPermissionId())
                .collect(Collectors.toList());

        List<Permission> permissions = permissionRepository.findByIdIn(permissionIds);
        List<String> codes = permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());

        logger.debug("[RbacService] Loaded {} permissions for Role ID: {}", codes.size(), roleId);
        return codes;
    }

    @Override
    @Transactional(value = "userTransactionManager", readOnly = true)
    public boolean hasPermission(Integer roleId, String permissionCode) {
        if (roleId == null || permissionCode == null || permissionCode.trim().isEmpty()) {
            return false;
        }

        List<String> permissions = getPermissionsForRole(roleId);
        return permissions.stream().anyMatch(p -> p.equalsIgnoreCase(permissionCode.trim()));
    }

    @Override
    @Transactional(value = "userTransactionManager", rollbackFor = Exception.class)
    public RolePermissionAssignResponse assignRolePermissions(RolePermissionAssignRequest request) {
        return processRolePermissionAssignment(request, "Role permissions assigned successfully");
    }

    @Override
    @Transactional(value = "userTransactionManager", rollbackFor = Exception.class)
    public RolePermissionAssignResponse updateRolePermissions(RolePermissionAssignRequest request) {
        return processRolePermissionAssignment(request, "Role permissions updated successfully");
    }

    private RolePermissionAssignResponse processRolePermissionAssignment(RolePermissionAssignRequest request, String successMessage) {
        if (request == null) {
            throw new RbacValidationException("Request payload cannot be null");
        }

        // 1. Validate Role ID exists in R001
        Integer roleId = request.getRoleId();
        if (roleId == null) {
            throw new RbacValidationException("Role ID is mandatory");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with ID: " + roleId));

        // Optionally update role description if provided
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            role.setDescription(request.getDescription().trim());
            roleRepository.save(role);
        }

        // 2. Validate totalSelected mandatory field
        if (request.getTotalSelected() == null) {
            throw new RbacValidationException("totalSelected is mandatory");
        }

        List<Integer> permissionIds = request.getPermissionIds() != null
                ? request.getPermissionIds()
                : Collections.emptyList();
        List<String> permissionCodes = request.getPermissionCodes() != null
                ? request.getPermissionCodes()
                : Collections.emptyList();

        // 3. Validate matching counts between permissionIds and permissionCodes
        if (permissionIds.size() != permissionCodes.size()) {
            throw new RbacValidationException("Count of permissionIds (" + permissionIds.size()
                    + ") does not match count of permissionCodes (" + permissionCodes.size() + ")");
        }

        // 4. Validate totalSelected matches count of submitted permissions
        if (request.getTotalSelected() != permissionIds.size()) {
            throw new RbacValidationException("totalSelected (" + request.getTotalSelected()
                    + ") does not match count of submitted permission IDs (" + permissionIds.size() + ")");
        }

        // 5. Prevent duplicate permission IDs
        Set<Integer> uniquePermissionIds = new LinkedHashSet<>(permissionIds);
        if (uniquePermissionIds.size() != permissionIds.size()) {
            throw new RbacValidationException("Duplicate permission IDs found in request");
        }

        // 6. Prevent duplicate permission codes (case-insensitive)
        Set<String> uniqueCodes = new LinkedHashSet<>();
        for (String rawCode : permissionCodes) {
            if (rawCode != null && !rawCode.trim().isEmpty()) {
                String normalized = rawCode.trim().toUpperCase();
                if (!uniqueCodes.add(normalized)) {
                    throw new RbacValidationException("Duplicate permission code found in request: " + rawCode.trim());
                }
            }
        }

        // 7. Process R002: Store exact frontend IDs and codes, reuse matching, reject conflicts
        Set<Integer> targetPermissionIds = new LinkedHashSet<>();

        for (int i = 0; i < permissionIds.size(); i++) {
            Integer pId = permissionIds.get(i);
            String rawCode = permissionCodes.get(i);

            if (pId == null) {
                throw new RbacValidationException("Permission ID at index " + i + " cannot be null");
            }
            if (rawCode == null || rawCode.trim().isEmpty()) {
                throw new RbacValidationException("Permission code at index " + i + " cannot be empty");
            }

            String pCode = rawCode.trim();

            Optional<Permission> permById = permissionRepository.findById(pId);
            Optional<Permission> permByCode = permissionRepository.findByCodeIgnoreCase(pCode);

            if (permById.isPresent() && permByCode.isPresent()) {
                if (permById.get().getId().equals(permByCode.get().getId())) {
                    // ID already exists with the same permission code -> reuse it
                    targetPermissionIds.add(pId);
                } else {
                    // Conflict: ID is mapped to another code, and code is mapped to another ID
                    throw new RbacValidationException("Conflict: Permission ID " + pId + " is already assigned to code '"
                            + permById.get().getCode() + "', but code '" + pCode + "' already exists under ID "
                            + permByCode.get().getId());
                }
            } else if (permById.isPresent()) {
                // ID exists with a different permission code -> conflict error
                throw new RbacValidationException("Conflict: Permission ID " + pId + " already exists in R002 with code '"
                        + permById.get().getCode() + "' (requested code: '" + pCode + "')");
            } else if (permByCode.isPresent()) {
                // Permission code exists under a different ID -> conflict error
                throw new RbacValidationException("Conflict: Permission code '" + pCode + "' already exists in R002 under ID "
                        + permByCode.get().getId() + " (requested ID: " + pId + ")");
            } else {
                // Store exact frontend permission ID and code into R002
                String module = pCode.contains(".") ? pCode.split("\\.")[0].toUpperCase() : "GENERAL";
                String description = pCode.replace(".", " - ");

                Permission newPerm = Permission.builder()
                        .id(pId)
                        .code(pCode)
                        .module(module)
                        .description(description)
                        .build();
                permissionRepository.save(newPerm);
                logger.info("[RBAC] Stored exact frontend permission in R002: ID={}, Code={}", pId, pCode);
                targetPermissionIds.add(pId);
            }
        }

        // 8. Synchronize Role-Permission Mappings in R003 using exact frontend permission IDs
        List<RolePermission> existingMappings = rolePermissionRepository.findByIdRoleId(roleId);
        Set<Integer> existingPermissionIds = existingMappings.stream()
                .map(m -> m.getId().getPermissionId())
                .collect(Collectors.toSet());

        // A. Remove mappings for permissions deselected by Super Admin
        List<RolePermission> toRemove = existingMappings.stream()
                .filter(m -> !targetPermissionIds.contains(m.getId().getPermissionId()))
                .collect(Collectors.toList());
        if (!toRemove.isEmpty()) {
            rolePermissionRepository.deleteAll(toRemove);
            logger.info("[RBAC] Removed {} deselected permissions for Role ID: {}", toRemove.size(), roleId);
        }

        // B. Insert newly selected permissions (retaining permissions that are already present)
        List<RolePermission> toInsert = targetPermissionIds.stream()
                .filter(pid -> !existingPermissionIds.contains(pid))
                .map(pid -> new RolePermission(new RolePermissionId(roleId, pid)))
                .collect(Collectors.toList());
        if (!toInsert.isEmpty()) {
            rolePermissionRepository.saveAll(toInsert);
            logger.info("[RBAC] Inserted {} new permissions for Role ID: {}", toInsert.size(), roleId);
        }

        int retainedCount = (int) existingPermissionIds.stream().filter(targetPermissionIds::contains).count();
        logger.info("[RBAC] Permission synchronization complete for Role ID: {}. Retained: {}, Inserted: {}, Removed: {}, Total Active: {}",
                roleId, retainedCount, toInsert.size(), toRemove.size(), targetPermissionIds.size());

        // 9. Return success response
        return RolePermissionAssignResponse.builder()
                .success(true)
                .message(successMessage)
                .roleId(role.getId())
                .roleName(role.getName())
                .totalSelected(targetPermissionIds.size())
                .build();
    }
}
