package com.los.bank.interestrate.controller;

import com.los.bank.interestrate.dto.*;
import com.los.bank.interestrate.service.InterestRateMasterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Interest Rate Master: effective-dated, tiered interest rates per loan product.
 *
 * <pre>
 * POST   /api/interest-rate-masters                 create a rate sheet (PENDING)
 * GET    /api/interest-rate-masters                 list (filter by productCode, status)
 * GET    /api/interest-rate-masters/{id}            one sheet with tiers
 * PUT    /api/interest-rate-masters/{id}            edit (only while not in effect; back to PENDING)
 * DELETE /api/interest-rate-masters/{id}            delete (only while not in effect)
 * POST   /api/interest-rate-masters/{id}/approve    checker approves (not the maker)
 * POST   /api/interest-rate-masters/{id}/reject     checker rejects with a reason
 * GET    /api/interest-rate-masters/applicable-rate rate for productCode + amount + tenureMonths
 * </pre>
 */
@RestController
@RequestMapping("/api/interest-rate-masters")
public class InterestRateMasterController {

    private static final String ADMIN = " or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')";

    private final InterestRateMasterService service;

    public InterestRateMasterController(InterestRateMasterService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INTEREST_RATE_CREATE')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<InterestRateMasterResponse>> create(
            @RequestBody InterestRateMasterRequest request, Authentication authentication) {
        InterestRateMasterResponse response = service.create(request, staffUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InterestRateApiResponse.success("Interest rate sheet created and sent for approval", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INTEREST_RATE_VIEW')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<InterestRatePageResponse<InterestRateMasterResponse>>> list(
            @RequestParam(name = "productCode", required = false) String productCode,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(InterestRateApiResponse.success("Interest rate sheets retrieved successfully",
                service.list(productCode, status, page, size)));
    }

    @GetMapping("/applicable-rate")
    @PreAuthorize("hasAuthority('INTEREST_RATE_VIEW') or hasAuthority('LOAN_APPLICATION_CREATE')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<ApplicableRateResponse>> applicableRate(
            @RequestParam(name = "productCode") String productCode,
            @RequestParam(name = "amount") BigDecimal amount,
            @RequestParam(name = "tenureMonths") Integer tenureMonths,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(InterestRateApiResponse.success("Applicable interest rate found",
                service.findApplicableRate(productCode, amount, tenureMonths, date)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEREST_RATE_VIEW')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<InterestRateMasterResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(InterestRateApiResponse.success("Interest rate sheet retrieved successfully", service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEREST_RATE_UPDATE')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<InterestRateMasterResponse>> update(
            @PathVariable("id") Long id, @RequestBody InterestRateMasterRequest request, Authentication authentication) {
        InterestRateMasterResponse response = service.update(id, request, staffUserId(authentication));
        return ResponseEntity.ok(InterestRateApiResponse.success("Interest rate sheet updated and sent for approval", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEREST_RATE_DELETE')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<Void>> delete(@PathVariable("id") Long id, Authentication authentication) {
        service.delete(id, staffUserId(authentication));
        return ResponseEntity.ok(InterestRateApiResponse.success("Interest rate sheet deleted successfully", null));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('INTEREST_RATE_APPROVE')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<InterestRateMasterResponse>> approve(
            @PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(InterestRateApiResponse.success("Interest rate sheet approved",
                service.approve(id, staffUserId(authentication))));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('INTEREST_RATE_APPROVE')" + ADMIN)
    public ResponseEntity<InterestRateApiResponse<InterestRateMasterResponse>> reject(
            @PathVariable("id") Long id,
            @RequestBody(required = false) InterestRateReviewRequest request,
            Authentication authentication) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(InterestRateApiResponse.success("Interest rate sheet rejected",
                service.reject(id, reason, staffUserId(authentication))));
    }

    private Long staffUserId(Authentication authentication) {
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
