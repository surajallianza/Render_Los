package com.los.bank.bankmaster.controller;

import com.los.bank.bankmaster.dto.BankAccountRequestDto;
import com.los.bank.bankmaster.dto.BankAccountResponseDto;
import com.los.bank.bankmaster.dto.BankMasterApiResponse;
import com.los.bank.bankmaster.service.BankAccountService;
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
@RequestMapping({"/api/bank-accounts", "/bank-accounts"})
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    /**
     * Create Bank Account (s001)
     * POST /api/bank-accounts or /bank-accounts
     */
    @PostMapping
    public ResponseEntity<BankMasterApiResponse<BankAccountResponseDto>> createBankAccount(
            @Valid @RequestBody BankAccountRequestDto requestDto) {
        BankAccountResponseDto created = bankAccountService.createBankAccount(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BankMasterApiResponse.success("Bank Account created successfully.", created));
    }

    /**
     * Get All Bank Accounts
     * GET /api/bank-accounts or /bank-accounts
     */
    @GetMapping
    public ResponseEntity<BankMasterApiResponse<List<BankAccountResponseDto>>> getAllBankAccounts(
            @RequestParam(value = "branchId", required = false) Long branchId) {
        List<BankAccountResponseDto> accounts = bankAccountService.getAllBankAccounts(branchId);
        return ResponseEntity.ok(BankMasterApiResponse.success("Bank Accounts retrieved successfully.", accounts));
    }

    /**
     * Get Bank Account By ID
     * GET /api/bank-accounts/{id} or /bank-accounts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BankMasterApiResponse<BankAccountResponseDto>> getBankAccountById(
            @PathVariable("id") Long id) {
        BankAccountResponseDto account = bankAccountService.getBankAccountById(id);
        return ResponseEntity.ok(BankMasterApiResponse.success("Bank Account retrieved successfully.", account));
    }

    /**
     * Update Bank Account
     * PUT /api/bank-accounts/{id} or /bank-accounts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BankMasterApiResponse<BankAccountResponseDto>> updateBankAccount(
            @PathVariable("id") Long id,
            @Valid @RequestBody BankAccountRequestDto requestDto) {
        BankAccountResponseDto updated = bankAccountService.updateBankAccount(id, requestDto);
        return ResponseEntity.ok(BankMasterApiResponse.success("Bank Account updated successfully.", updated));
    }

    /**
     * Delete Bank Account (Soft Delete)
     * DELETE /api/bank-accounts/{id} or /bank-accounts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BankMasterApiResponse<Void>> deleteBankAccount(
            @PathVariable("id") Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.ok(BankMasterApiResponse.success("Bank Account deleted successfully.", null));
    }
}
