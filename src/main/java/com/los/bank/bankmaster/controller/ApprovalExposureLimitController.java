package com.los.bank.bankmaster.controller;

import com.los.bank.bankmaster.dto.ApprovalLimitRequestDto;
import com.los.bank.bankmaster.dto.ApprovalLimitResponseDto;
import com.los.bank.bankmaster.dto.BankMasterApiResponse;
import com.los.bank.bankmaster.service.ApprovalExposureLimitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/approval-limits", "/approval-limits"})
public class ApprovalExposureLimitController {

    private final ApprovalExposureLimitService approvalLimitService;

    public ApprovalExposureLimitController(ApprovalExposureLimitService approvalLimitService) {
        this.approvalLimitService = approvalLimitService;
    }

    /**
     * Create Approval & Exposure Limit (s002)
     * POST /api/approval-limits or /approval-limits
     */
    @PostMapping
    public ResponseEntity<BankMasterApiResponse<ApprovalLimitResponseDto>> createApprovalLimit(
            @Valid @RequestBody ApprovalLimitRequestDto requestDto) {
        ApprovalLimitResponseDto created = approvalLimitService.createApprovalLimit(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BankMasterApiResponse.success("Approval & Exposure Limit created successfully.", created));
    }

    /**
     * Get All Approval & Exposure Limits
     * GET /api/approval-limits or /approval-limits
     */
    @GetMapping
    public ResponseEntity<BankMasterApiResponse<List<ApprovalLimitResponseDto>>> getAllApprovalLimits(
            @RequestParam(value = "branchId", required = false) Long branchId) {
        List<ApprovalLimitResponseDto> limits = approvalLimitService.getAllApprovalLimits(branchId);
        return ResponseEntity.ok(BankMasterApiResponse.success("Approval & Exposure Limits retrieved successfully.", limits));
    }

    /**
     * Get Approval & Exposure Limit By ID
     * GET /api/approval-limits/{id} or /approval-limits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BankMasterApiResponse<ApprovalLimitResponseDto>> getApprovalLimitById(
            @PathVariable("id") Long id) {
        ApprovalLimitResponseDto limit = approvalLimitService.getApprovalLimitById(id);
        return ResponseEntity.ok(BankMasterApiResponse.success("Approval & Exposure Limit retrieved successfully.", limit));
    }

    /**
     * Update Approval & Exposure Limit
     * PUT /api/approval-limits/{id} or /approval-limits/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BankMasterApiResponse<ApprovalLimitResponseDto>> updateApprovalLimit(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApprovalLimitRequestDto requestDto) {
        ApprovalLimitResponseDto updated = approvalLimitService.updateApprovalLimit(id, requestDto);
        return ResponseEntity.ok(BankMasterApiResponse.success("Approval & Exposure Limit updated successfully.", updated));
    }

    /**
     * Delete Approval & Exposure Limit (Soft Delete)
     * DELETE /api/approval-limits/{id} or /approval-limits/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BankMasterApiResponse<Void>> deleteApprovalLimit(
            @PathVariable("id") Long id) {
        approvalLimitService.deleteApprovalLimit(id);
        return ResponseEntity.ok(BankMasterApiResponse.success("Approval & Exposure Limit deleted successfully.", null));
    }
}
