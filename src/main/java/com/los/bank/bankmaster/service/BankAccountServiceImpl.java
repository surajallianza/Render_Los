package com.los.bank.bankmaster.service;

import com.los.bank.bankmaster.dto.BankAccountRequestDto;
import com.los.bank.bankmaster.dto.BankAccountResponseDto;
import com.los.bank.bankmaster.entity.BankAccount;
import com.los.bank.bankmaster.exception.BankMasterException;
import com.los.bank.bankmaster.repository.BankAccountRepository;
import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private static final Logger logger = LoggerFactory.getLogger(BankAccountServiceImpl.class);

    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern ACCOUNT_NUM_PATTERN = Pattern.compile("^[0-9A-Za-z]{6,34}$");

    private final BankAccountRepository bankAccountRepository;
    private final BranchRepository branchRepository;

    public BankAccountServiceImpl(
            @Autowired(required = false) BankAccountRepository bankAccountRepository,
            @Autowired(required = false) BranchRepository branchRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    public BankAccountResponseDto createBankAccount(BankAccountRequestDto requestDto) {
        validateRequest(requestDto);

        String accNum = requestDto.getAccountNumber().trim();
        String ifsc = requestDto.getIfscCode().trim().toUpperCase();
        Long branchId = requestDto.getBranchId();

        if (bankAccountRepository != null && bankAccountRepository.existsActiveAccount(accNum, ifsc, branchId)) {
            throw new BankMasterException(
                    "Active bank account with account number '" + accNum + "' and IFSC '" + ifsc + "' already exists.",
                    HttpStatus.CONFLICT
            );
        }

        String branchName = resolveBranchName(branchId);

        BankAccount entity = BankAccount.builder()
                .bankId(requestDto.getBankId())
                .bankName(requestDto.getBankName().trim())
                .branchId(branchId)
                .particulars(requestDto.getParticulars() != null ? requestDto.getParticulars().trim() : null)
                .particularsLookupId(requestDto.getParticularsLookupId())
                .accountType(requestDto.getAccountType().trim())
                .accountTypeLookupId(requestDto.getAccountTypeLookupId())
                .accountNumber(accNum)
                .micrCode(requestDto.getMicrCode() != null ? requestDto.getMicrCode().trim() : null)
                .ifscCode(ifsc)
                .isActive(true)
                .status(requestDto.getStatus() != null ? requestDto.getStatus().trim() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        BankAccount saved = bankAccountRepository != null ? bankAccountRepository.save(entity) : entity;
        logger.info("Successfully created BankAccount with ID: {}", saved.getId());
        return mapToResponseDto(saved, branchName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponseDto> getAllBankAccounts(Long branchId) {
        if (bankAccountRepository == null) {
            return List.of();
        }

        List<BankAccount> accounts;
        if (branchId != null) {
            accounts = bankAccountRepository.findByBranchIdAndIsActiveTrue(branchId);
        } else {
            accounts = bankAccountRepository.findByIsActiveTrueOrderByIdDesc();
        }

        return accounts.stream()
                .map(acc -> mapToResponseDto(acc, resolveBranchName(acc.getBranchId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountResponseDto getBankAccountById(Long id) {
        if (id == null) {
            throw new BankMasterException("Bank Account ID cannot be null.", HttpStatus.BAD_REQUEST);
        }
        if (bankAccountRepository == null) {
            throw new BankMasterException("Repository not initialized.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        BankAccount account = bankAccountRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BankMasterException("Bank Account not found with ID: " + id, HttpStatus.NOT_FOUND));

        return mapToResponseDto(account, resolveBranchName(account.getBranchId()));
    }

    @Override
    @Transactional
    public BankAccountResponseDto updateBankAccount(Long id, BankAccountRequestDto requestDto) {
        if (id == null) {
            throw new BankMasterException("Bank Account ID cannot be null.", HttpStatus.BAD_REQUEST);
        }
        validateRequest(requestDto);

        if (bankAccountRepository == null) {
            throw new BankMasterException("Repository not initialized.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        BankAccount existing = bankAccountRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BankMasterException("Bank Account not found with ID: " + id, HttpStatus.NOT_FOUND));

        String accNum = requestDto.getAccountNumber().trim();
        String ifsc = requestDto.getIfscCode().trim().toUpperCase();
        Long branchId = requestDto.getBranchId();

        if (bankAccountRepository.existsActiveAccountExcludingId(accNum, ifsc, branchId, id)) {
            throw new BankMasterException(
                    "Another active bank account with account number '" + accNum + "' and IFSC '" + ifsc + "' already exists.",
                    HttpStatus.CONFLICT
            );
        }

        String branchName = resolveBranchName(branchId);

        existing.setBankId(requestDto.getBankId());
        existing.setBankName(requestDto.getBankName().trim());
        existing.setBranchId(branchId);
        existing.setParticulars(requestDto.getParticulars() != null ? requestDto.getParticulars().trim() : null);
        existing.setParticularsLookupId(requestDto.getParticularsLookupId());
        existing.setAccountType(requestDto.getAccountType().trim());
        existing.setAccountTypeLookupId(requestDto.getAccountTypeLookupId());
        existing.setAccountNumber(accNum);
        existing.setMicrCode(requestDto.getMicrCode() != null ? requestDto.getMicrCode().trim() : null);
        existing.setIfscCode(ifsc);
        if (requestDto.getStatus() != null && !requestDto.getStatus().trim().isEmpty()) {
            existing.setStatus(requestDto.getStatus().trim());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        BankAccount updated = bankAccountRepository.save(existing);
        logger.info("Successfully updated BankAccount with ID: {}", updated.getId());
        return mapToResponseDto(updated, branchName);
    }

    @Override
    @Transactional
    public void deleteBankAccount(Long id) {
        if (id == null) {
            throw new BankMasterException("Bank Account ID cannot be null.", HttpStatus.BAD_REQUEST);
        }
        if (bankAccountRepository == null) {
            throw new BankMasterException("Repository not initialized.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        BankAccount existing = bankAccountRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BankMasterException("Bank Account not found with ID: " + id, HttpStatus.NOT_FOUND));

        // Soft delete
        existing.setIsActive(false);
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        bankAccountRepository.save(existing);
        logger.info("Soft-deleted BankAccount with ID: {}", id);
    }

    private void validateRequest(BankAccountRequestDto dto) {
        if (dto == null) {
            throw new BankMasterException("Request body cannot be null.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
            throw new BankMasterException("Bank name is required.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getAccountType() == null || dto.getAccountType().trim().isEmpty()) {
            throw new BankMasterException("Account type is required.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getAccountNumber() == null || dto.getAccountNumber().trim().isEmpty()) {
            throw new BankMasterException("Account number is required.", HttpStatus.BAD_REQUEST);
        }
        if (!ACCOUNT_NUM_PATTERN.matcher(dto.getAccountNumber().trim()).matches()) {
            throw new BankMasterException("Account number must be between 6 and 34 alphanumeric characters.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getIfscCode() == null || dto.getIfscCode().trim().isEmpty()) {
            throw new BankMasterException("IFSC code is required.", HttpStatus.BAD_REQUEST);
        }
        if (!IFSC_PATTERN.matcher(dto.getIfscCode().trim().toUpperCase()).matches()) {
            throw new BankMasterException("Please enter a valid 11-character IFSC code (e.g. SBIN0001234).", HttpStatus.BAD_REQUEST);
        }
        if (dto.getBranchId() != null && branchRepository != null) {
            branchRepository.findById(dto.getBranchId()).orElseThrow(() ->
                    new BankMasterException("Invalid branch reference. Branch not found with ID: " + dto.getBranchId(), HttpStatus.BAD_REQUEST));
        }
    }

    private String resolveBranchName(Long branchId) {
        if (branchId == null || branchRepository == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .map(Branch::getName)
                .orElse(null);
    }

    private BankAccountResponseDto mapToResponseDto(BankAccount e, String branchName) {
        return BankAccountResponseDto.builder()
                .id(e.getId())
                .bankId(e.getBankId())
                .bankName(e.getBankName())
                .branchId(e.getBranchId())
                .branchName(branchName)
                .particulars(e.getParticulars())
                .particularsLookupId(e.getParticularsLookupId())
                .accountType(e.getAccountType())
                .accountTypeLookupId(e.getAccountTypeLookupId())
                .accountNumber(e.getAccountNumber())
                .micrCode(e.getMicrCode())
                .ifscCode(e.getIfscCode())
                .isActive(e.getIsActive())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
