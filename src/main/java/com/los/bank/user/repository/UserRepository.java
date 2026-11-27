package com.los.bank.user.repository;

import com.los.bank.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.profile " +
           "LEFT JOIN FETCH u.securitySetting " +
           "ORDER BY u.id ASC")
    List<User> findAllWithDetails();

    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.profile " +
           "LEFT JOIN FETCH u.securitySetting " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmpNo(String empNo);

    boolean existsByUsername(String username);

    boolean existsByEmpNo(String empNo);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.username = :username AND u.id <> :id")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.empNo = :empNo AND u.id <> :id")
    boolean existsByEmpNoAndIdNot(@Param("empNo") String empNo, @Param("id") Long id);
}
