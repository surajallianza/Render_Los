package com.los.bank.product.service;

import com.los.bank.product.dto.LoanProductRequestDto;
import com.los.bank.product.dto.LoanProductResponseDto;

import java.util.Map;

/**
 * Service interface defining core business operations for the Loan Product Module.
 */
public interface LoanProductService {

    /**
     * Creates and persists a new Loan Product and its delegation records.
     */
    LoanProductResponseDto createLoanProduct(LoanProductRequestDto requestDto);

    /**
     * Retrieves all loan products with pagination and search filtering.
     */
    Map<String, Object> getAllLoanProducts(int page, int size, String search);

    /**
     * Retrieves full loan product details by primary key ID.
     */
    LoanProductResponseDto getLoanProductById(Long id);

    /**
     * Retrieves full loan product details by product code.
     */
    LoanProductResponseDto getLoanProductByCode(String code);

    /**
     * Updates an existing loan product and synchronizes its delegation records.
     */
    LoanProductResponseDto updateLoanProduct(Long id, LoanProductRequestDto requestDto);
}
