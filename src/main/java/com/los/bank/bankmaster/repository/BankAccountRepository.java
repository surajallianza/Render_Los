package com.los.bank.bankmaster.repository;

import com.los.bank.bankmaster.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByIsActiveTrueOrderByIdDesc();

    Optional<BankAccount> findByIdAndIsActiveTrue(Long id);

    List<BankAccount> findByBranchIdAndIsActiveTrue(Long branchId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BankAccount b " +
           "WHERE b.accountNumber = :accountNumber AND b.ifscCode = :ifscCode " +
           "AND (:branchId IS NULL AND b.branchId IS NULL OR b.branchId = :branchId) " +
           "AND b.isActive = true")
    boolean existsActiveAccount(
            @Param("accountNumber") String accountNumber,
            @Param("ifscCode") String ifscCode,
            @Param("branchId") Long branchId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BankAccount b " +
           "WHERE b.accountNumber = :accountNumber AND b.ifscCode = :ifscCode " +
           "AND (:branchId IS NULL AND b.branchId IS NULL OR b.branchId = :branchId) " +
           "AND b.id <> :id AND b.isActive = true")
    boolean existsActiveAccountExcludingId(
            @Param("accountNumber") String accountNumber,
            @Param("ifscCode") String ifscCode,
            @Param("branchId") Long branchId,
            @Param("id") Long id);
}
