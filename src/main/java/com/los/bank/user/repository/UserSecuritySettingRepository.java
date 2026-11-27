package com.los.bank.user.repository;

import com.los.bank.user.entity.UserSecuritySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSecuritySettingRepository extends JpaRepository<UserSecuritySetting, Long> {

    Optional<UserSecuritySetting> findByUserId(Long userId);
}
