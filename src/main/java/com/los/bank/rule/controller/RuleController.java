package com.los.bank.rule.controller;

import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.dto.RuleStatusRequest;
import com.los.bank.rule.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rule Engine: loan eligibility rules. Check / When value is / Then come from Lookup Master
 * ("Rule Parameter", "Rule Operator", "Rule Decision").
 *
 * <pre>
 * GET    /api/rules/options        dropdown values (parameters, operators, decisions, choice options)
 * POST   /api/rules                create a rule (applies from its effective date, no approval)
 * GET    /api/rules                list (filters: status, param, decision)
 * GET    /api/rules/{id}           one rule
 * PUT    /api/rules/{id}           update a rule
 * PATCH  /api/rules/{id}/status    activate / deactivate
 * DELETE /api/rules/{id}           remove a rule
 * </pre>
 * Also served under /api/rule-engine.
 */
@RestController
@RequestMapping({"/api/rules", "/api/rule-engine"})
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping({"/options", "/parameters"})
    public ResponseEntity<Map<String, Object>> getOptions() {
        return ResponseEntity.ok(body("Rule options retrieved successfully", ruleService.getOptions()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody RuleRequestDto requestDto, Authentication authentication) {
        RuleResponseDto created = ruleService.createRule(requestDto, staffUserId(authentication));
        Map<String, Object> response = body("Rule created", created);
        response.put("id", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRules(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "param", required = false) String param,
            @RequestParam(value = "decision", required = false) String decision) {
        return ResponseEntity.ok(ruleService.getAllRules(page, size, status, param, decision));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRuleById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(body(null, ruleService.getRuleById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable("id") Long id,
                                                          @RequestBody RuleRequestDto requestDto,
                                                          Authentication authentication) {
        return ResponseEntity.ok(body("Rule updated", ruleService.updateRule(id, requestDto, staffUserId(authentication))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable("id") Long id,
                                                            @RequestBody RuleStatusRequest request,
                                                            Authentication authentication) {
        RuleResponseDto updated = ruleService.changeStatus(id, request != null ? request.getStatus() : null,
                staffUserId(authentication));
        return ResponseEntity.ok(body("Rule status updated to " + updated.getStatus(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable("id") Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(body("Rule removed", null));
    }

    private static Map<String, Object> body(String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        if (message != null) {
            response.put("message", message);
        }
        response.put("data", data);
        return response;
    }

    private static Long staffUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
