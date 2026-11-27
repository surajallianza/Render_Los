package com.los.bank.bankmaster.service;

import com.los.bank.bankmaster.dto.ApprovalLimitRequestDto;
import com.los.bank.bankmaster.dto.ApprovalLimitResponseDto;

import java.util.List;

public interface ApprovalExposureLimitService {

    ApprovalLimitResponseDto createApprovalLimit(ApprovalLimitRequestDto requestDto);

    List<ApprovalLimitResponseDto> getAllApprovalLimits(Long branchId);

    ApprovalLimitResponseDto getApprovalLimitById(Long id);

    ApprovalLimitResponseDto updateApprovalLimit(Long id, ApprovalLimitRequestDto requestDto);

    void deleteApprovalLimit(Long id);
}
