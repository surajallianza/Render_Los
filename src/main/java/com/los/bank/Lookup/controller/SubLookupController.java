package com.los.bank.lookup.controller;

import com.los.bank.lookup.dto.StatusUpdateRequest;
import com.los.bank.lookup.dto.SubLookupResponse;
import com.los.bank.lookup.dto.SubLookupUpdateRequest;
import com.los.bank.lookup.service.LookupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for independent Sub Lookup operations.
 */
@RestController
@RequestMapping("/api/sub-lookups")
public class SubLookupController {

    private final LookupService lookupService;

    public SubLookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    /**
     * Creates a new sub lookup directly via request payload.
     * Payload specifies parent lookup via 'code' (parent ID) or 'lookup_code' (e.g. 10008)
     * and sub lookup code via 'sub_lookup_code' (e.g. 1000801).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSubLookup(@Valid @RequestBody com.los.bank.lookup.dto.SubLookupCreateRequest request) {
        SubLookupResponse data = lookupService.createSubLookup((String) null, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sub lookup created successfully");
        response.put("data", data);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing sub lookup (name, description, status, displayOrder).
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSubLookup(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubLookupUpdateRequest request) {
        SubLookupResponse data = lookupService.updateSubLookup(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sub lookup updated successfully");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles status between Active and Inactive for a sub lookup.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleSubLookupStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        SubLookupResponse data = lookupService.toggleSubLookupStatus(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sub lookup status updated successfully");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
