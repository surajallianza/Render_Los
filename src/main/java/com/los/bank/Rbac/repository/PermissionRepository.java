package com.los.bank.Rbac.repository;

import com.los.bank.Rbac.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByCodeIgnoreCase(String code);

    List<Permission> findByIdIn(Collection<Integer> ids);
}
