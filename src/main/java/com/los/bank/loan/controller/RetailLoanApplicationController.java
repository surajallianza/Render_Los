package com.los.bank.loan.controller;

import com.los.bank.loan.dto.RetailLoanApplicationEnvelopeDto;
import com.los.bank.loan.dto.RetailLoanFormDto;
import com.los.bank.loan.exception.RetailLoanValidationException;
import com.los.bank.loan.service.RetailLoanApplicationService;
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
 * REST Controller exposing Retail Loan Application Module APIs:
 * - GET  /api/retail-loan-applications
 * - GET  /api/retail-loan-applications/{id}
 * - POST /api/retail-loan-applications
 * - PUT  /api/retail-loan-applications/{id}
 */
@RestController
@RequestMapping("/api/retail-loan-applications")
public class RetailLoanApplicationController {

    private final RetailLoanApplicationService applicationService;

    public RetailLoanApplicationController(RetailLoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * GET /api/retail-loan-applications
     * Retrieves paginated retail loan applications with optional status and search filtering.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(applicationService.getAllApplications(page, size, status, search));
    }

    /**
     * GET /api/retail-loan-applications/{id}
     * Retrieves a single retail loan application by its primary key ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApplicationById(@PathVariable("id") Long id) {
        RetailLoanFormDto form = applicationService.getApplicationById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", RetailLoanApplicationEnvelopeDto.builder().retailLoanForm(form).build());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/retail-loan-applications
     * Creates a new retail loan application, enforcing EMI/Bullet Payment rules.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createApplication(
            @RequestBody RetailLoanApplicationEnvelopeDto requestEnvelope) {
        if (requestEnvelope == null || requestEnvelope.getRetailLoanForm() == null) {
            throw new RetailLoanValidationException("retailLoanForm object is required.");
        }

        RetailLoanFormDto created = applicationService.createApplication(requestEnvelope.getRetailLoanForm());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Retail loan application created successfully");
        response.put("id", created.getId());
        response.put("data", RetailLoanApplicationEnvelopeDto.builder().retailLoanForm(created).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/retail-loan-applications/{id}
     * Updates an existing retail loan application, re-evaluating EMI/Bullet Payment rules.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateApplication(
            @PathVariable("id") Long id,
            @RequestBody RetailLoanApplicationEnvelopeDto requestEnvelope) {
        if (requestEnvelope == null || requestEnvelope.getRetailLoanForm() == null) {
            throw new RetailLoanValidationException("retailLoanForm object is required.");
        }

        RetailLoanFormDto updated = applicationService.updateApplication(id, requestEnvelope.getRetailLoanForm());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Retail loan application updated successfully");
        response.put("id", updated.getId());
        response.put("data", RetailLoanApplicationEnvelopeDto.builder().retailLoanForm(updated).build());
        return ResponseEntity.ok(response);
    }
}
