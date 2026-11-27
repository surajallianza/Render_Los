package com.los.bank.branch.repository;

import com.los.bank.branch.entity.Branch;

import java.util.List;
import java.util.Optional;

public interface BranchRepository {

    Optional<Branch> findById(Long id);

    Optional<Branch> findById(Long id, String bankName);

    Optional<Branch> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Branch> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code, String bankName);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id, String bankName);

    List<Branch> findAll();

    List<Branch> findAll(String bankName);

    Branch save(Branch branch);
}
