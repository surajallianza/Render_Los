package com.los.bank.lookup.controller;

import com.los.bank.lookup.dto.*;
import com.los.bank.lookup.service.LookupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Lookup Management (Masters > Lookup page).
 */
@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    /**
     * Lists lookups with optional search and status filter.
     * Each lookup includes its embedded sub lookups for UI row expansion.
     * Returns total record count for the badge.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> searchLookups(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status) {
        List<LookupResponse> list = lookupService.searchLookups(search, status);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", list.size());
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single lookup by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLookupById(@PathVariable("id") Long id) {
        LookupResponse data = lookupService.getLookupById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new lookup master record.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLookup(@Valid @RequestBody LookupCreateRequest request) {
        LookupResponse data = lookupService.createLookup(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lookup created successfully");
        response.put("data", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing lookup (name, description, status).
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateLookup(
            @PathVariable("id") Long id,
            @Valid @RequestBody LookupUpdateRequest request) {
        LookupResponse data = lookupService.updateLookup(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lookup updated successfully");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles status between Active and Inactive for a lookup.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleLookupStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        LookupResponse data = lookupService.toggleLookupStatus(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lookup status updated successfully");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // PARENT-NESTED SUB LOOKUP ENDPOINTS
    // =====================================================================

    /**
     * Retrieves all sub lookups belonging to a parent lookup (by numeric ID or lookup code).
     */
    @GetMapping("/{lookupIdentifier}/sub-lookups")
    public ResponseEntity<Map<String, Object>> getSubLookupsByLookupIdentifier(@PathVariable("lookupIdentifier") String lookupIdentifier) {
        List<SubLookupResponse> list = lookupService.getSubLookupsByLookupIdentifier(lookupIdentifier);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", list.size());
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new sub lookup under a parent lookup (by numeric ID or lookup code).
     */
    @PostMapping("/{lookupIdentifier}/sub-lookups")
    public ResponseEntity<Map<String, Object>> createSubLookup(
            @PathVariable("lookupIdentifier") String lookupIdentifier,
            @Valid @RequestBody SubLookupCreateRequest request) {
        SubLookupResponse data = lookupService.createSubLookup(lookupIdentifier, request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sub lookup created successfully");
        response.put("data", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
