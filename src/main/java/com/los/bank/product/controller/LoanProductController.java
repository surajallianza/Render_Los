package com.los.bank.product.controller;

import com.los.bank.product.dto.LoanProductRequestDto;
import com.los.bank.product.dto.LoanProductResponseDto;
import com.los.bank.product.service.LoanProductService;
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
 * REST Controller exposing Loan Product Module endpoints:
 * - GET  /api/loan-products: Fetch all loan products (with pagination/search)
 * - GET  /api/loan-products/{id}: Fetch loan product by primary key ID
 * - GET  /api/loan-products/code/{code}: Fetch loan product by unique product code
 * - POST /api/loan-products: Create a new loan product
 * - PUT  /api/loan-products/{id}: Update an existing loan product
 */
@RestController
@RequestMapping("/api/loan-products")
public class LoanProductController {

    private final LoanProductService loanProductService;

    public LoanProductController(LoanProductService loanProductService) {
        this.loanProductService = loanProductService;
    }

    /**
     * POST /api/loan-products
     * Creates a new loan product along with its delegation of power records.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLoanProduct(@RequestBody LoanProductRequestDto requestDto) {
        LoanProductResponseDto created = loanProductService.createLoanProduct(requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Loan product created successfully");
        response.put("id", created.getId());
        response.put("data", created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/loan-products
     * Fetches all loan products with pagination and optional search filter.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLoanProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search) {

        Map<String, Object> result = loanProductService.getAllLoanProducts(page, size, search);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/loan-products/{id}
     * Fetches complete loan product details by primary key ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLoanProductById(@PathVariable("id") Long id) {
        LoanProductResponseDto product = loanProductService.getLoanProductById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", product);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/loan-products/code/{code}
     * Fetches complete loan product details by unique product code.
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getLoanProductByCode(@PathVariable("code") String code) {
        LoanProductResponseDto product = loanProductService.getLoanProductByCode(code);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", product);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/loan-products/{id}
     * Updates an existing loan product and synchronizes its delegation records.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateLoanProduct(
            @PathVariable("id") Long id,
            @RequestBody LoanProductRequestDto requestDto) {

        LoanProductResponseDto updated = loanProductService.updateLoanProduct(id, requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Loan product updated successfully");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }
}
