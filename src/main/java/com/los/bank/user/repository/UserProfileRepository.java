package com.los.bank.user.repository;

import com.los.bank.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM UserProfile p " +
           "WHERE p.email = :email AND p.userId <> :userId")
    boolean existsByEmailAndUserIdNot(@Param("email") String email, @Param("userId") Long userId);
}
