package com.los.bank.branch.service;

import com.los.bank.branch.dto.BranchDto;
import com.los.bank.branch.dto.BranchRequestDto;

import java.util.List;

public interface BranchService {

    BranchDto createBranch(BranchRequestDto requestDto);

    BranchDto updateBranch(Long id, BranchRequestDto requestDto);

    List<BranchDto> getAllBranches();

    BranchDto getBranchById(Long id);
}
