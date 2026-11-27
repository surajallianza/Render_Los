package com.los.bank.bankmaster.service;

import com.los.bank.bankmaster.dto.ApprovalLimitRequestDto;
import com.los.bank.bankmaster.dto.ApprovalLimitResponseDto;
import com.los.bank.bankmaster.entity.ApprovalExposureLimit;
import com.los.bank.bankmaster.exception.BankMasterException;
import com.los.bank.bankmaster.repository.ApprovalExposureLimitRepository;
import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalExposureLimitServiceImpl implements ApprovalExposureLimitService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalExposureLimitServiceImpl.class);

    private final ApprovalExposureLimitRepository limitRepository;
    private final BranchRepository branchRepository;

    public ApprovalExposureLimitServiceImpl(
            @Autowired(required = false) ApprovalExposureLimitRepository limitRepository,
            @Autowired(required = false) BranchRepository branchRepository) {
        this.limitRepository = limitRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    public ApprovalLimitResponseDto createApprovalLimit(ApprovalLimitRequestDto requestDto) {
        validateRequest(requestDto);

        String authority = requestDto.getApprovalAuthority().trim();
        Long branchId = requestDto.getBranchId();

        if (limitRepository != null && limitRepository.existsActiveAuthority(authority, branchId)) {
            throw new BankMasterException(
                    "Active approval limit record for authority '" + authority + "' already exists for this branch/bank.",
                    HttpStatus.CONFLICT
            );
        }

        String branchName = resolveBranchName(branchId);

        ApprovalExposureLimit entity = ApprovalExposureLimit.builder()
                .bankId(requestDto.getBankId())
                .bankName(requestDto.getBankName() != null ? requestDto.getBankName().trim() : null)
                .branchId(branchId)
                .approvalAuthority(authority)
                .approvalAuthorityLookupId(requestDto.getApprovalAuthorityLookupId())
                .singlePartyLimit(requestDto.getSinglePartyLimit())
                .groupExposureLimit(requestDto.getGroupExposureLimit())
                .sanctionMargin(requestDto.getSanctionMargin())
                .penaltyCharge(requestDto.getPenaltyCharge())
                .penaltyRate(requestDto.getPenaltyRate())
                .isActive(true)
                .status(requestDto.getStatus() != null ? requestDto.getStatus().trim() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        ApprovalExposureLimit saved = limitRepository != null ? limitRepository.save(entity) : entity;
        logger.info("Successfully created ApprovalExposureLimit with ID: {}", saved.getId());
        return mapToResponseDto(saved, branchName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalLimitResponseDto> getAllApprovalLimits(Long branchId) {
        if (limitRepository == null) {
            return List.of();
        }

        List<ApprovalExposureLimit> limits;
        if (branchId != null) {
            limits = limitRepository.findByBranchIdAndIsActiveTrue(branchId);
        } else {
            limits = limitRepository.findByIsActiveTrueOrderByIdDesc();
        }

        return limits.stream()
                .map(l -> mapToResponseDto(l, resolveBranchName(l.getBranchId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalLimitResponseDto getApprovalLimitById(Long id) {
        if (id == null) {
            throw new BankMasterException("Approval Limit ID cannot be null.", HttpStatus.BAD_REQUEST);
        }
        if (limitRepository == null) {
            throw new BankMasterException("Repository not initialized.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        ApprovalExposureLimit limit = limitRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BankMasterException("Approval & Exposure Limit record not found with ID: " + id, HttpStatus.NOT_FOUND));

        return mapToResponseDto(limit, resolveBranchName(limit.getBranchId()));
    }

    @Override
    @Transactional
    public ApprovalLimitResponseDto updateApprovalLimit(Long id, ApprovalLimitRequestDto requestDto) {
        if (id == null) {
            throw new BankMasterException("Approval Limit ID cannot be null.", HttpStatus.BAD_REQUEST);
        }
        validateRequest(requestDto);

        if (limitRepository == null) {
            throw new BankMasterException("Repository not initialized.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        ApprovalExposureLimit existing = limitRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BankMasterException("Approval & Exposure Limit record not found with ID: " + id, HttpStatus.NOT_FOUND));

        String authority = requestDto.getApprovalAuthority().trim();
        Long branchId = requestDto.getBranchId();

        if (limitRepository.existsActiveAuthorityExcludingId(authority, branchId, id)) {
            throw new BankMasterException(
                    "Another active limit record for authority '" + authority + "' already exists for this branch/bank.",
                    HttpStatus.CONFLICT
            );
        }

        String branchName = resolveBranchName(branchId);

        existing.setBankId(requestDto.getBankId());
        if (requestDto.getBankName() != null) {
            existing.setBankName(requestDto.getBankName().trim());
        }
        existing.setBranchId(branchId);
        existing.setApprovalAuthority(authority);
        existing.setApprovalAuthorityLookupId(requestDto.getApprovalAuthorityLookupId());
        existing.setSinglePartyLimit(requestDto.getSinglePartyLimit());
        existing.setGroupExposureLimit(requestDto.getGroupExposureLimit());
        existing.setSanctionMargin(requestDto.getSanctionMargin());
        existing.setPenaltyCharge(requestDto.getPenaltyCharge());
        existing.setPenaltyRate(requestDto.getPenaltyRate());
        if (requestDto.getStatus() != null && !requestDto.getStatus().trim().isEmpty()) {
            existing.setStatus(requestDto.getStatus().trim());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        ApprovalExposureLimit updated = limitRepository.save(existing);
        logger.info("Successfully updated ApprovalExposureLimit with ID: {}", updated.getId());
        return mapToResponseDto(updated, branchName);
    }

    @Override
    @Transactional
    public void deleteApprovalLimit(Long id) {
        if (id == null) {
            throw new BankMasterException("Approval Limit ID cannot be null.", HttpStatus.BAD_REQUEST);
        }
        if (limitRepository == null) {
            throw new BankMasterException("Repository not initialized.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        ApprovalExposureLimit existing = limitRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BankMasterException("Approval & Exposure Limit record not found with ID: " + id, HttpStatus.NOT_FOUND));

        // Soft delete
        existing.setIsActive(false);
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        limitRepository.save(existing);
        logger.info("Soft-deleted ApprovalExposureLimit with ID: {}", id);
    }

    private void validateRequest(ApprovalLimitRequestDto dto) {
        if (dto == null) {
            throw new BankMasterException("Request body cannot be null.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getApprovalAuthority() == null || dto.getApprovalAuthority().trim().isEmpty()) {
            throw new BankMasterException("Approval authority is required.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getSinglePartyLimit() == null || dto.getSinglePartyLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BankMasterException("Single party limit must be non-negative.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getGroupExposureLimit() == null || dto.getGroupExposureLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BankMasterException("Group exposure limit must be non-negative.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getSanctionMargin() == null || dto.getSanctionMargin().compareTo(BigDecimal.ZERO) < 0) {
            throw new BankMasterException("Sanction margin must be non-negative.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getPenaltyCharge() == null || dto.getPenaltyCharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new BankMasterException("Penalty charge must be non-negative.", HttpStatus.BAD_REQUEST);
        }
        if (dto.getPenaltyRate() == null || dto.getPenaltyRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BankMasterException("Penalty rate must be non-negative.", HttpStatus.BAD_REQUEST);
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

    private ApprovalLimitResponseDto mapToResponseDto(ApprovalExposureLimit e, String branchName) {
        return ApprovalLimitResponseDto.builder()
                .id(e.getId())
                .bankId(e.getBankId())
                .bankName(e.getBankName())
                .branchId(e.getBranchId())
                .branchName(branchName)
                .approvalAuthority(e.getApprovalAuthority())
                .approvalAuthorityLookupId(e.getApprovalAuthorityLookupId())
                .singlePartyLimit(e.getSinglePartyLimit())
                .groupExposureLimit(e.getGroupExposureLimit())
                .sanctionMargin(e.getSanctionMargin())
                .penaltyCharge(e.getPenaltyCharge())
                .penaltyRate(e.getPenaltyRate())
                .isActive(e.getIsActive())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
