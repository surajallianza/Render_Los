package com.los.bank.rule.controller;

import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller exposing Rule Engine Module endpoints:
 * - POST /api/rules: Create a new business rule
 * - GET  /api/rules: Retrieve all configured rules (with optional pagination, status, param filters)
 * - GET  /api/rules/{id}: Retrieve a specific rule by primary key ID
 * - PUT  /api/rules/{id}: Update an existing rule by primary key ID
 */
@RestController
@RequestMapping({"/api/rules", "/api/rule-engine"})
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * POST /api/rules
     * Creates and persists a new configurable business rule.
     *
     * @param requestDto Rule configuration payload
     * @return 201 CREATED with created rule details
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody RuleRequestDto requestDto) {
        RuleResponseDto created = ruleService.createRule(requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Rule created successfully");
        response.put("id", created.getId());
        response.put("data", created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/rules
     * Retrieves all configured rules with optional filtering and pagination.
     *
     * @param page Page index (0-based)
     * @param size Page size
     * @param status Optional status filter ('active', 'inactive')
     * @param param Optional parameter name filter
     * @return 200 OK with rule list and pagination metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRules(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "param", required = false) String param) {

        Map<String, Object> result = ruleService.getAllRules(page, size, status, param);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rules/{id}
     * Retrieves a specific configured rule by its unique identifier.
     *
     * @param id Rule primary key ID
     * @return 200 OK with the rule details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRuleById(@PathVariable("id") Long id) {
        RuleResponseDto rule = ruleService.getRuleById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rule);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/rules/{id}
     * Updates an existing rule by its primary key ID.
     *
     * @param id Rule primary key ID
     * @param requestDto Updated rule configuration payload
     * @return 200 OK with the updated rule details
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(
            @PathVariable("id") Long id,
            @RequestBody RuleRequestDto requestDto) {

        RuleResponseDto updated = ruleService.updateRule(id, requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Rule updated successfully");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }
}
