package com.los.bank.user.repository;

import com.los.bank.user.entity.UserBranchId;
import com.los.bank.user.entity.UserBranchMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBranchMappingRepository extends JpaRepository<UserBranchMapping, UserBranchId> {

    List<UserBranchMapping> findByUserId(Long userId);

    Optional<UserBranchMapping> findByUserIdAndBranchId(Long userId, Long branchId);

    void deleteByUserId(Long userId);
}
