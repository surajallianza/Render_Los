package com.los.bank.goldrate.controller;

import com.los.bank.goldrate.dto.GoldRateActivityDto;
import com.los.bank.goldrate.dto.GoldRateApiResponse;
import com.los.bank.goldrate.dto.GoldRateMasterDto;
import com.los.bank.goldrate.service.GoldRateService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;

/**
 * REST Controller exposing Gold Rate Master and Complete Audit Trail / Activity APIs:
 * - Activity Records:
 *   - GET  /api/gold-rates/activities : Get all activity records
 *   - GET  /api/gold-rates/activities/{id} : Get activity by ID
 *   - POST /api/gold-rates/activities : Create a new activity record
 *   - PUT  /api/gold-rates/activities/{id} : Update activity record (maker-checker verification)
 * - Master Rates:
 *   - GET  /api/gold-rates/master : Get all Gold Rate Master records
 *   - GET  /api/gold-rates/master/{recordId} : Get Gold Rate Master by recordId
 *   - POST /api/gold-rates/master : Create new Gold Rate Master record
 *   - PUT  /api/gold-rates/master/{recordId} : Update Gold Rate Master (auto-creates audit record with old/new values)
 */
@RestController
@RequestMapping("/api/gold-rates")
public class GoldRateController {

    private final GoldRateService goldRateService;

    public GoldRateController(GoldRateService goldRateService) {
        this.goldRateService = goldRateService;
    }

    // =========================================================================
    // ACTIVITY AUDIT TRAIL ENDPOINTS
    // =========================================================================

    /**
     * Retrieves all Gold Rate Master activity records.
     * Supports optional query filtering by recordId and status.
     */
    @GetMapping("/activities")
    public ResponseEntity<GoldRateApiResponse<List<GoldRateActivityDto>>> getAllActivities(
            @RequestParam(value = "recordId", required = false) String recordId,
            @RequestParam(value = "status", required = false) String status) {
        List<GoldRateActivityDto> activities = goldRateService.getAllActivities(recordId, status);
        return ResponseEntity.ok(GoldRateApiResponse.success("Activity records retrieved successfully", activities));
    }

    /**
     * Retrieves a specific Gold Rate Master activity record by ID.
     */
    @GetMapping("/activities/{id}")
    public ResponseEntity<GoldRateApiResponse<GoldRateActivityDto>> getActivityById(@PathVariable("id") String id) {
        GoldRateActivityDto activity = goldRateService.getActivityById(id);
        return ResponseEntity.ok(GoldRateApiResponse.success("Activity record retrieved successfully", activity));
    }

    /**
     * Records a new activity for Gold Rate Master.
     */
    @PostMapping("/activities")
    public ResponseEntity<GoldRateApiResponse<GoldRateActivityDto>> createActivity(
            @RequestBody GoldRateActivityDto requestDto,
            HttpServletRequest request) {
        GoldRateActivityDto created = goldRateService.createActivity(requestDto, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GoldRateApiResponse.success("Activity record created successfully", created));
    }

    /**
     * Updates an activity record for verification / maker-checker workflow while preserving immutable audit details.
     */
    @PutMapping("/activities/{id}")
    public ResponseEntity<GoldRateApiResponse<GoldRateActivityDto>> updateActivity(
            @PathVariable("id") String id,
            @RequestBody GoldRateActivityDto requestDto,
            HttpServletRequest request) {
        GoldRateActivityDto updated = goldRateService.updateActivity(id, requestDto, request);
        return ResponseEntity.ok(GoldRateApiResponse.success("Activity record updated successfully", updated));
    }

    // =========================================================================
    // GOLD RATE MASTER ENDPOINTS
    // =========================================================================

    /**
     * Retrieves all Gold Rate Master records.
     */
    @GetMapping("/master")
    public ResponseEntity<GoldRateApiResponse<List<GoldRateMasterDto>>> getAllMasterRates() {
        List<GoldRateMasterDto> rates = goldRateService.getAllMasterRates();
        return ResponseEntity.ok(GoldRateApiResponse.success("Gold rates retrieved successfully", rates));
    }

    /**
     * Retrieves a specific Gold Rate Master record by recordId (e.g. GOLD-1029-12).
     */
    @GetMapping("/master/{recordId}")
    public ResponseEntity<GoldRateApiResponse<GoldRateMasterDto>> getMasterRateByRecordId(@PathVariable("recordId") String recordId) {
        GoldRateMasterDto rate = goldRateService.getMasterRateByRecordId(recordId);
        return ResponseEntity.ok(GoldRateApiResponse.success("Gold rate retrieved successfully", rate));
    }

    /**
     * Creates a new Gold Rate Master record and generates an initial audit entry.
     */
    @PostMapping("/master")
    public ResponseEntity<GoldRateApiResponse<GoldRateMasterDto>> createMasterRate(
            @RequestBody GoldRateMasterDto requestDto,
            HttpServletRequest request) {
        GoldRateMasterDto created = goldRateService.createMasterRate(requestDto, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GoldRateApiResponse.success("Gold rate created successfully", created));
    }

    /**
     * Updates a Gold Rate Master record and automatically captures old and new values in the audit trail.
     */
    @PutMapping("/master/{recordId}")
    public ResponseEntity<GoldRateApiResponse<GoldRateMasterDto>> updateMasterRate(
            @PathVariable("recordId") String recordId,
            @RequestBody GoldRateMasterDto requestDto,
            HttpServletRequest request) {
        GoldRateMasterDto updated = goldRateService.updateMasterRate(recordId, requestDto, request);
        return ResponseEntity.ok(GoldRateApiResponse.success("Gold rate updated successfully with audit trail", updated));
    }
}
