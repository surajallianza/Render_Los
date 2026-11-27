package com.los.bank.branch.controller;

import com.los.bank.branch.dto.BranchApiResponse;
import com.los.bank.branch.dto.BranchDto;
import com.los.bank.branch.dto.BranchRequestDto;
import com.los.bank.branch.service.BranchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller exposing Bank Branch Management APIs:
 * - POST /api/bank-profile/branch: Create a new branch
 * - GET  /api/bank-profile/branch: Retrieve all branches
 * - GET  /api/bank-profile/branch/{id}: Retrieve branch details by ID
 */
@RestController
@RequestMapping("/api/bank-profile/branch")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * Creates and saves a new bank branch.
     *
     * @param requestDto Submitted branch details
     * @return 201 CREATED with the saved branch information
     */
    @PostMapping
    public ResponseEntity<BranchApiResponse<BranchDto>> createBranch(@RequestBody BranchRequestDto requestDto) {
        BranchDto created = branchService.createBranch(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BranchApiResponse.success("Branch created successfully", created));
    }

    /**
     * Updates an existing bank branch by primary key ID.
     *
     * @param id Branch primary key ID
     * @param requestDto Updated branch details
     * @return 200 OK with the updated branch information
     */
    @PutMapping("/{id}")
    public ResponseEntity<BranchApiResponse<BranchDto>> updateBranch(
            @PathVariable("id") Long id,
            @RequestBody BranchRequestDto requestDto) {
        BranchDto updated = branchService.updateBranch(id, requestDto);
        return ResponseEntity.ok(BranchApiResponse.success("Branch updated successfully", updated));
    }

    /**
     * Updates an existing bank branch when ID is provided within the request payload.
     *
     * @param requestDto Updated branch details containing ID
     * @return 200 OK with the updated branch information
     */
    @PutMapping
    public ResponseEntity<BranchApiResponse<BranchDto>> updateBranch(
            @RequestBody BranchRequestDto requestDto) {
        Long id = requestDto.getId();
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(BranchApiResponse.error("Branch ID is required in request body for update."));
        }
        BranchDto updated = branchService.updateBranch(id, requestDto);
        return ResponseEntity.ok(BranchApiResponse.success("Branch updated successfully", updated));
    }

    /**
     * Retrieves all existing bank branches.
     *
     * @return 200 OK with list of all branches
     */
    @GetMapping
    public ResponseEntity<BranchApiResponse<List<BranchDto>>> getAllBranches() {
        List<BranchDto> branches = branchService.getAllBranches();
        return ResponseEntity.ok(BranchApiResponse.success("Branches retrieved successfully", branches));
    }

    /**
     * Retrieves a single branch's details by primary key ID.
     *
     * @param id Branch primary key ID
     * @return 200 OK with branch details or 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<BranchApiResponse<BranchDto>> getBranchById(@PathVariable("id") Long id) {
        BranchDto branch = branchService.getBranchById(id);
        return ResponseEntity.ok(BranchApiResponse.success("Branch retrieved successfully", branch));
    }
}
