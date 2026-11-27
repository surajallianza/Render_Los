package com.los.bank.bankmaster.service;

import com.los.bank.bankmaster.dto.BankAccountRequestDto;
import com.los.bank.bankmaster.dto.BankAccountResponseDto;

import java.util.List;

public interface BankAccountService {

    BankAccountResponseDto createBankAccount(BankAccountRequestDto requestDto);

    List<BankAccountResponseDto> getAllBankAccounts(Long branchId);

    BankAccountResponseDto getBankAccountById(Long id);

    BankAccountResponseDto updateBankAccount(Long id, BankAccountRequestDto requestDto);

    void deleteBankAccount(Long id);
}
