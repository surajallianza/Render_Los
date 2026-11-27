package com.los.bank.setup.controller;

import com.los.bank.setup.dto.BankDto;
import com.los.bank.setup.dto.BankSetupRequestDto;
import com.los.bank.setup.dto.BankSetupResponseDto;
import com.los.bank.setup.dto.InitialUserDto;
import com.los.bank.setup.service.BankSetupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// REST Controller exposing Bank Setup and Bank Management APIs.
@RestController
@RequestMapping("/api/setup")
public class BankSetupController {

    private final BankSetupService bankSetupService;

    public BankSetupController(BankSetupService bankSetupService) {
        this.bankSetupService = bankSetupService;
    }

    // Initializes a new bank and its dedicated database, runs Flyway migrations, and seeds super admin.
    @PostMapping("/initialize-bank")
    public ResponseEntity<BankSetupResponseDto> initializeBank(@RequestBody BankSetupRequestDto requestDto) {
        BankSetupResponseDto response = bankSetupService.initializeBank(requestDto);
        return ResponseEntity.ok(response);
    }

    // Retrieves all registered banks with complete registration details.
    @GetMapping("/banks")
    public ResponseEntity<Map<String, Object>> getAllBanks() {
        List<BankDto> banks = bankSetupService.getAllBanks();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", banks.size());
        response.put("data", banks);
        return ResponseEntity.ok(response);
    }

    // Retrieves a single bank by ID.
    @GetMapping("/banks/{id}")
    public ResponseEntity<Map<String, Object>> getBankById(@PathVariable("id") Long id) {
        BankDto bank = bankSetupService.getBankById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", bank);
        return ResponseEntity.ok(response);
    }

    // Retrieves a single bank by Bank Code.
    @GetMapping("/banks/code/{bankCode}")
    public ResponseEntity<Map<String, Object>> getBankByCode(@PathVariable("bankCode") String bankCode) {
        BankDto bank = bankSetupService.getBankByCode(bankCode);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", bank);
        return ResponseEntity.ok(response);
    }

    // Updates an existing bank's registration information.
    @PutMapping("/banks/{id}")
    public ResponseEntity<Map<String, Object>> updateBank(
            @PathVariable("id") Long id,
            @RequestBody BankDto updateDto) {
        BankDto updatedBank = bankSetupService.updateBank(id, updateDto);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bank details updated successfully");
        response.put("data", updatedBank);
        return ResponseEntity.ok(response);
    }

    // Retrieves the initial administrator user profile for a given bank (without password).
    @GetMapping("/banks/{id}/user")
    public ResponseEntity<Map<String, Object>> getBankInitialUser(@PathVariable("id") Long id) {
        InitialUserDto user = bankSetupService.getBankInitialUser(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", user);
        return ResponseEntity.ok(response);
    }
}
