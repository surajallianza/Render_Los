package com.los.bank.setup.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.los.bank.setup.dto.OrganizationDto;
import com.los.bank.setup.service.OrganizationService;

/**
 * Controller for Bank / Organization Administration APIs: - GET
 * /api/organizations/{organizationId} : Load organization/bank details - PATCH
 * /api/organizations/{organizationId} : Update organization/bank details - PUT
 * /api/organizations/{organizationId} : Update organization/bank details
 */
@RestController
@RequestMapping({"/bank", "/api/bank", "/api/organizations"})
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * GET /bank or /bank/{organizationId} or /api/organizations/{organizationId}
     * Retrieves organization / bank details including accounts.
     *
     * @param organizationId Either numeric ID (e.g. 1) or Bank Code (e.g. 10010). Defaults to "1" if omitted.
     */
    @GetMapping({"", "/", "/{organizationId}"})
    public ResponseEntity<Map<String, Object>> getOrganization(
            @PathVariable(value = "organizationId", required = false) String organizationId) {
        String targetId = (organizationId != null && !organizationId.trim().isEmpty()) ? organizationId.trim() : "1";
        OrganizationDto dto = organizationService.getOrganization(targetId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Organization details retrieved successfully");
        response.put("data", dto);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /bank or /bank/{organizationId} or /api/organizations/{organizationId}
     * Partially or fully updates organization / bank details and accounts.
     */
    @PatchMapping({"", "/", "/{organizationId}"})
    public ResponseEntity<Map<String, Object>> updateOrganizationPatch(
            @PathVariable(value = "organizationId", required = false) String organizationId,
            @RequestBody OrganizationDto updateDto) {
        String targetId = (organizationId != null && !organizationId.trim().isEmpty())
                ? organizationId.trim()
                : (updateDto.getBankCode() != null ? updateDto.getBankCode().trim() : "1");
        OrganizationDto updated = organizationService.updateOrganization(targetId, updateDto);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Organization details updated successfully");
        response.put("data", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /bank or /bank/{organizationId} or /api/organizations/{organizationId}
     * Provided for clients that send PUT instead of PATCH.
     */
    @PutMapping({"", "/", "/{organizationId}"})
    public ResponseEntity<Map<String, Object>> updateOrganizationPut(
            @PathVariable(value = "organizationId", required = false) String organizationId,
            @RequestBody OrganizationDto updateDto) {
        return updateOrganizationPatch(organizationId, updateDto);
    }
}
