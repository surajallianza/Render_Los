package com.los.bank.loanEligibility.controller;

import com.los.bank.loanEligibility.dto.EligibilityApiResponse;
import com.los.bank.loanEligibility.dto.EligibilityRequest;
import com.los.bank.loanEligibility.dto.EligibilityResponse;
import com.los.bank.loanEligibility.service.LoanEligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Loan eligibility.
 * <pre>
 * POST /api/loan-eligibility/assess   check eligibility (also POST /api/loan-eligibility)
 * GET  /api/loan-eligibility/{id}     one saved check
 * GET  /api/loan-eligibility?leadId=  saved checks, newest first
 * </pre>
 */
@RestController
@RequestMapping("/api/loan-eligibility")
public class LoanEligibilityController {

    private final LoanEligibilityService service;

    public LoanEligibilityController(LoanEligibilityService service) {
        this.service = service;
    }

    @PostMapping({"/assess", ""})
    public ResponseEntity<EligibilityApiResponse<EligibilityResponse>> assess(@RequestBody EligibilityRequest request,
                                                                             Authentication authentication) {
        EligibilityResponse response = service.assess(request, staffUserId(authentication));
        String message = response.isEligible()
                ? "Customer is eligible for the requested loan"
                : "Customer is not eligible for the requested loan";
        return ResponseEntity.ok(EligibilityApiResponse.success(message, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EligibilityApiResponse<EligibilityResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(EligibilityApiResponse.success("Eligibility check retrieved successfully", service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<EligibilityApiResponse<Map<String, Object>>> list(
            @RequestParam(name = "leadId", required = false) String leadId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(EligibilityApiResponse.success("Eligibility checks retrieved successfully",
                service.list(leadId, page, size)));
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
