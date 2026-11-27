package com.los.bank.lookup.controller;

import com.los.bank.lookup.dto.LookupDropdownItem;
import com.los.bank.lookup.service.LookupConsumerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consumer REST Controller used by loan applications, onboarding, and other forms
 * to load dropdown options and bulk dropdown mappings.
 */
@RestController
@RequestMapping("/api/lookups")
public class LookupConsumerController {

    private final LookupConsumerService lookupConsumerService;

    public LookupConsumerController(LookupConsumerService lookupConsumerService) {
        this.lookupConsumerService = lookupConsumerService;
    }

    /**
     * Single dropdown endpoint:
     * GET /api/lookups/code/{lookupCode}/sub-lookups?activeOnly=true
     * Returns [{ "code": "1", "name": "Gold Loan", "displayOrder": 1 }, ...] sorted by display_order.
     * Returns an empty list if the lookup is inactive or does not exist.
     */
    @GetMapping("/code/{lookupCode}/sub-lookups")
    public ResponseEntity<List<LookupDropdownItem>> getDropdownByLookupCode(
            @PathVariable("lookupCode") String lookupCode,
            @RequestParam(value = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        List<LookupDropdownItem> items = lookupConsumerService.getDropdownOptions(lookupCode, activeOnly);
        return ResponseEntity.ok(items);
    }

    /**
     * Bulk dropdown endpoint:
     * GET /api/lookups/sub-lookups?codes=10004,10005,10002&activeOnly=true
     * Returns a map: { "10004": [ {code, name, displayOrder}, ... ], "10005": [ ... ] }.
     * Unknown lookup codes return an empty list, not an error.
     */
    @GetMapping("/sub-lookups")
    public ResponseEntity<Map<String, List<LookupDropdownItem>>> getBulkDropdowns(
            @RequestParam("codes") String codesParam,
            @RequestParam(value = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        if (codesParam == null || codesParam.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        List<String> codes = Arrays.stream(codesParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Map<String, List<LookupDropdownItem>> resultMap = lookupConsumerService.getBulkDropdownOptions(codes, activeOnly);
        return ResponseEntity.ok(resultMap);
    }
}
