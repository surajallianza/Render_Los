package com.los.bank.lead.controller;

import com.los.bank.lead.dto.LeadAssignRequestDto;
import com.los.bank.lead.dto.LeadDto;
import com.los.bank.lead.service.LeadCsvService;
import com.los.bank.lead.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * REST controller exposing all Lead Module APIs.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final LeadCsvService leadCsvService;

    public LeadController(LeadService leadService, LeadCsvService leadCsvService) {
        this.leadService = leadService;
        this.leadCsvService = leadCsvService;
    }

    /**
     * POST /api/leads
     * Creates a new lead with complete personal, KYC, loan, employment, address, and facility details.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLead(@RequestBody LeadDto requestDto) {
        LeadDto created = leadService.createLead(requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lead created successfully");
        response.put("leadId", created.getLeadId());
        response.put("data", created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/leads
     * Retrieves all leads with pagination and search/status filtering.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLeads(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {

        Map<String, Object> result = leadService.getAllLeads(page, size, status, search);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/leads/{leadId}
     * Retrieves full lead details by lead ID.
     */
    @GetMapping("/{leadId}")
    public ResponseEntity<Map<String, Object>> getLeadById(@PathVariable("leadId") String leadId) {
        LeadDto lead = leadService.getLeadById(leadId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", lead);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/leads/{leadId}
     * Updates an existing lead and its child collections.
     */
    @PutMapping("/{leadId}")
    public ResponseEntity<Map<String, Object>> updateLead(
            @PathVariable("leadId") String leadId,
            @RequestBody LeadDto requestDto) {

        LeadDto updated = leadService.updateLead(leadId, requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lead updated successfully");
        response.put("data", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/leads/{leadId}/assign
     * Assigns a lead to an employee after verifying employee existence.
     */
    @PutMapping("/{leadId}/assign")
    public ResponseEntity<Map<String, Object>> assignLead(
            @PathVariable("leadId") String leadId,
            @RequestBody LeadAssignRequestDto assignDto) {

        LeadDto assigned = leadService.assignLead(leadId, assignDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lead assigned successfully");
        response.put("data", assigned);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/leads/sample-csv
     * Downloads a sample CSV template for bulk lead import.
     */
    @GetMapping(value = "/sample-csv", produces = "text/csv")
    public ResponseEntity<byte[]> downloadSampleCsv() {
        byte[] csvData = leadCsvService.generateSampleCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("leads_sample.csv", StandardCharsets.UTF_8)
                .build());
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    /**
     * GET /api/leads/export
     * Exports all or filtered leads to a downloadable CSV file.
     */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportLeads(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search) {
        byte[] csvData = leadCsvService.exportLeads(status, search);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("leads_export.csv", StandardCharsets.UTF_8)
                .build());
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    /**
     * POST /api/leads/import
     * Imports multiple leads from an uploaded CSV file or raw CSV payload.
     */
    @PostMapping(value = "/import", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            "text/csv",
            MediaType.APPLICATION_OCTET_STREAM_VALUE
    })
    public ResponseEntity<Map<String, Object>> importLeads(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) byte[] rawBody,
            HttpServletRequest request) {

        byte[] bytes = null;
        if (file != null && !file.isEmpty()) {
            try {
                bytes = file.getBytes();
            } catch (Exception ex) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Failed to read uploaded file: " + ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
        } else if (rawBody != null && rawBody.length > 0) {
            bytes = rawBody;
        } else if (request instanceof MultipartHttpServletRequest multipartRequest) {
            Iterator<String> fileNames = multipartRequest.getFileNames();
            if (fileNames.hasNext()) {
                MultipartFile mf = multipartRequest.getFile(fileNames.next());
                if (mf != null && !mf.isEmpty()) {
                    try {
                        bytes = mf.getBytes();
                    } catch (Exception ignored) {}
                }
            }
        } else if (request != null) {
            try {
                bytes = request.getInputStream().readAllBytes();
            } catch (Exception ignored) {}
        }

        if (bytes == null || bytes.length == 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "CSV file or content is empty. Please provide a valid CSV file.");
            err.put("totalRows", 0);
            err.put("successfulImports", 0);
            err.put("failedImports", 0);
            err.put("successfulLeads", Collections.emptyList());
            err.put("errors", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        Map<String, Object> result = leadCsvService.importLeads(bytes);
        HttpStatus httpStatus = HttpStatus.OK;
        if (result.containsKey("totalRows") && ((Integer) result.get("totalRows")) == 0) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (result.containsKey("successfulImports") && ((Integer) result.get("successfulImports")) == 0) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(httpStatus).body(result);
    }
}
