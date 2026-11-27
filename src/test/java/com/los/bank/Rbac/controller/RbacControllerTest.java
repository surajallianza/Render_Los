package com.los.bank.Rbac.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.los.bank.Rbac.dto.RolePermissionAssignRequest;
import com.los.bank.Rbac.dto.RolePermissionAssignResponse;
import com.los.bank.Rbac.exception.RbacExceptionHandler;
import com.los.bank.Rbac.exception.RoleNotFoundException;
import com.los.bank.Rbac.repository.RoleRepository;
import com.los.bank.Rbac.service.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RbacControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RbacService rbacService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private com.los.bank.Rbac.repository.PermissionRepository permissionRepository;

    @InjectMocks
    private RbacController rbacController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rbacController)
                .setControllerAdvice(new RbacExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/rbac/roles/permissions should return 200 with expected response")
    void testPostAssignPermissions_Success() throws Exception {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(3)
                .roleName("CHECKER")
                .description("Checker role description")
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

        RolePermissionAssignResponse mockResponse = RolePermissionAssignResponse.builder()
                .success(true)
                .message("Role permissions assigned successfully")
                .roleId(3)
                .roleName("CHECKER")
                .totalSelected(5)
                .build();

        when(rbacService.assignRolePermissions(any(RolePermissionAssignRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/rbac/roles/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role permissions assigned successfully"))
                .andExpect(jsonPath("$.roleId").value(3))
                .andExpect(jsonPath("$.roleName").value("CHECKER"))
                .andExpect(jsonPath("$.totalSelected").value(5));
    }

    @Test
    @DisplayName("POST /api/rbac/roles/permissions should return 404 when role does not exist")
    void testPostAssignPermissions_RoleNotFound_Returns404() throws Exception {
        RolePermissionAssignRequest request = RolePermissionAssignRequest.builder()
                .roleId(999)
                .totalSelected(0)
                .permissionIds(Collections.emptyList())
                .permissionCodes(Collections.emptyList())
                .build();

        when(rbacService.assignRolePermissions(any(RolePermissionAssignRequest.class)))
                .thenThrow(new RoleNotFoundException("Role not found with ID: 999"));

        mockMvc.perform(post("/api/rbac/roles/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Role not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/rbac/roles/3/permissions should still return permissions (no breakage)")
    void testGetPermissionsForRole_ExistingApiPreserved() throws Exception {
        when(rbacService.getPermissionsForRole(3))
                .thenReturn(Arrays.asList("CUSTOMER.ONBOARDING.VIEW", "LOAN.APPLICATION.CREATE"));

        mockMvc.perform(get("/api/rbac/roles/3/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("CUSTOMER.ONBOARDING.VIEW"))
                .andExpect(jsonPath("$.data[1]").value("LOAN.APPLICATION.CREATE"));
    }
}
