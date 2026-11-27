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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceAssignmentTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RbacServiceImpl rbacService;

    private Role checkerRole;
    private List<Permission> mockPermissions;

    @BeforeEach
    void setUp() {
        checkerRole = Role.builder()
                .id(3)
                .name("CHECKER")
                .description("Checker role")
                .build();

        mockPermissions = Arrays.asList(
                Permission.builder().id(1).code("CUSTOMER.ONBOARDING.VIEW").module("CUSTOMER").build(),
                Permission.builder().id(6).code("LOAN.APPLICATION.CREATE").module("LOAN").build(),
                Permission.builder().id(7).code("LOAN.APPLICATION.UPDATE").module("LOAN").build(),
                Permission.builder().id(12).code("ADMIN.USER_MGMT.UPDATE").module("ADMIN").build(),
                Permission.builder().id(13).code("ADMIN.USER_MGMT.DELETE").module("ADMIN").build()
        );
    }

    @Test
    @DisplayName("Should reuse existing R002 permissions when IDs and codes already exist")
    void testAssignPermissions_ReuseExistingPermissions() {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .roleName("CHECKER")
                .description("Checker updated description")
                .totalSelected(5)
                .permissionIds(Arrays.asList(1, 6, 7, 12, 13))
                .permissionCodes(Arrays.asList(
                        "CUSTOMER.ONBOARDING.VIEW",
                        "LOAN.APPLICATION.CREATE",
                        "LOAN.APPLICATION.UPDATE",
                        "ADMIN.USER_MGMT.UPDATE",
                        "ADMIN.USER_MGMT.DELETE"
                ))
                .build();

        when(roleRepository.findById(3)).thenReturn(Optional.of(checkerRole));
        for (Permission p : mockPermissions) {
            when(permissionRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(permissionRepository.findByCodeIgnoreCase(p.getCode())).thenReturn(Optional.of(p));
        }
        when(rolePermissionRepository.findByIdRoleId(3)).thenReturn(Collections.emptyList());

        RolePermissionAssignResponse response = rbacService.assignRolePermissions(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Role permissions assigned successfully", response.getMessage());
        assertEquals(3, response.getRoleId());
        assertEquals("CHECKER", response.getRoleName());
        assertEquals(5, response.getTotalSelected());

        verify(rolePermissionRepository).saveAll(anyCollection());
        verify(roleRepository).save(checkerRole);
    }

    @Test
    @DisplayName("Should store exact frontend IDs 50 to 70 into R002 when new")
    void testAssignPermissions_StoreExactFrontendIdsIntoR002() {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .roleName("CHECKER")
                .totalSelected(2)
                .permissionIds(Arrays.asList(50, 51))
                .permissionCodes(Arrays.asList("NEW.ACTION.FIFTY", "NEW.ACTION.FIFTY_ONE"))
                .build();

        when(roleRepository.findById(3)).thenReturn(Optional.of(checkerRole));
        when(permissionRepository.findById(50)).thenReturn(Optional.empty());
        when(permissionRepository.findByCodeIgnoreCase("NEW.ACTION.FIFTY")).thenReturn(Optional.empty());
        when(permissionRepository.findById(51)).thenReturn(Optional.empty());
        when(permissionRepository.findByCodeIgnoreCase("NEW.ACTION.FIFTY_ONE")).thenReturn(Optional.empty());
        when(rolePermissionRepository.findByIdRoleId(3)).thenReturn(Collections.emptyList());

        RolePermissionAssignResponse response = rbacService.assignRolePermissions(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getTotalSelected());

        ArgumentCaptor<Permission> permCaptor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository, times(2)).save(permCaptor.capture());
        List<Permission> savedPerms = permCaptor.getAllValues();

        assertEquals(50, savedPerms.get(0).getId());
        assertEquals("NEW.ACTION.FIFTY", savedPerms.get(0).getCode());
        assertEquals(51, savedPerms.get(1).getId());
        assertEquals("NEW.ACTION.FIFTY_ONE", savedPerms.get(1).getCode());

        ArgumentCaptor<List<RolePermission>> rpCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionRepository).saveAll(rpCaptor.capture());
        List<RolePermission> savedMappings = rpCaptor.getValue();
        assertEquals(50, savedMappings.get(0).getId().getPermissionId());
        assertEquals(51, savedMappings.get(1).getId().getPermissionId());
    }

    @Test
    @DisplayName("Should return conflict error when ID exists with different code")
    void testAssignPermissions_ConflictIdExistsWithDifferentCode() {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .totalSelected(1)
                .permissionIds(Collections.singletonList(1))
                .permissionCodes(Collections.singletonList("DIFFERENT.CODE"))
                .build();

        when(roleRepository.findById(3)).thenReturn(Optional.of(checkerRole));
        when(permissionRepository.findById(1)).thenReturn(Optional.of(mockPermissions.get(0)));
        when(permissionRepository.findByCodeIgnoreCase("DIFFERENT.CODE")).thenReturn(Optional.empty());

        RbacValidationException ex = assertThrows(RbacValidationException.class, () ->
                rbacService.assignRolePermissions(request)
        );
        assertTrue(ex.getMessage().contains("Conflict: Permission ID 1 already exists in R002 with code"));
    }

    @Test
    @DisplayName("Should return conflict error when code exists under different ID")
    void testAssignPermissions_ConflictCodeExistsUnderDifferentId() {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .totalSelected(1)
                .permissionIds(Collections.singletonList(99)) // ID 99 sent
                .permissionCodes(Collections.singletonList("CUSTOMER.ONBOARDING.VIEW")) // already exists as ID 1
                .build();

        when(roleRepository.findById(3)).thenReturn(Optional.of(checkerRole));
        when(permissionRepository.findById(99)).thenReturn(Optional.empty());
        when(permissionRepository.findByCodeIgnoreCase("CUSTOMER.ONBOARDING.VIEW"))
                .thenReturn(Optional.of(mockPermissions.get(0))); // has ID 1

        RbacValidationException ex = assertThrows(RbacValidationException.class, () ->
                rbacService.assignRolePermissions(request)
        );
        assertTrue(ex.getMessage().contains("Conflict: Permission code 'CUSTOMER.ONBOARDING.VIEW' already exists in R002 under ID 1"));
    }

    @Test
    @DisplayName("Should throw RoleNotFoundException when role does not exist in R001")
    void testAssignPermissions_RoleNotFound() {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(999)
                .totalSelected(0)
                .permissionIds(Collections.emptyList())
                .permissionCodes(Collections.emptyList())
                .build();

        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        RoleNotFoundException ex = assertThrows(RoleNotFoundException.class, () ->
                rbacService.assignRolePermissions(request)
        );
        assertTrue(ex.getMessage().contains("Role not found with ID: 999"));
        verify(rolePermissionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw RbacValidationException when duplicate permission codes exist")
    void testAssignPermissions_DuplicateCodes() {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .totalSelected(3)
                .permissionIds(Arrays.asList(1, 2, 3))
                .permissionCodes(Arrays.asList("LOAN.VIEW", "LOAN.CREATE", "LOAN.VIEW"))
                .build();

        when(roleRepository.findById(3)).thenReturn(Optional.of(checkerRole));

        RbacValidationException ex = assertThrows(RbacValidationException.class, () ->
                rbacService.assignRolePermissions(request)
        );
        assertTrue(ex.getMessage().contains("Duplicate permission code"));
    }

    @Test
    @DisplayName("Should differentially synchronize R003: retain matching, delete deselected, insert new")
    @SuppressWarnings("unchecked")
    void testAssignPermissions_DifferentialUpdate() {
        List<RolePermission> existing = Arrays.asList(
                new RolePermission(new RolePermissionId(3, 1)),
                new RolePermission(new RolePermissionId(3, 6))
        );

        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .totalSelected(2)
                .permissionIds(Arrays.asList(6, 7))
                .permissionCodes(Arrays.asList("LOAN.APPLICATION.CREATE", "LOAN.APPLICATION.UPDATE"))
                .build();

        when(roleRepository.findById(3)).thenReturn(Optional.of(checkerRole));
        when(permissionRepository.findById(6)).thenReturn(Optional.of(mockPermissions.get(1)));
        when(permissionRepository.findByCodeIgnoreCase("LOAN.APPLICATION.CREATE"))
                .thenReturn(Optional.of(mockPermissions.get(1)));
        when(permissionRepository.findById(7)).thenReturn(Optional.of(mockPermissions.get(2)));
        when(permissionRepository.findByCodeIgnoreCase("LOAN.APPLICATION.UPDATE"))
                .thenReturn(Optional.of(mockPermissions.get(2)));
        when(rolePermissionRepository.findByIdRoleId(3)).thenReturn(existing);

        RolePermissionAssignResponse response = rbacService.assignRolePermissions(request);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getTotalSelected());

        // Verify deselected (ID 1) was deleted
        ArgumentCaptor<List<RolePermission>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionRepository).deleteAll(deleteCaptor.capture());
        List<RolePermission> deleted = deleteCaptor.getValue();
        assertEquals(1, deleted.size());
        assertEquals(1, deleted.get(0).getId().getPermissionId());

        // Verify newly selected (ID 7) was saved
        ArgumentCaptor<List<RolePermission>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionRepository).saveAll(saveCaptor.capture());
        List<RolePermission> saved = saveCaptor.getValue();
        assertEquals(1, saved.size());
        assertEquals(7, saved.get(0).getId().getPermissionId());
    }
}
