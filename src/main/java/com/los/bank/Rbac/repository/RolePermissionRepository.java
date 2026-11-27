package com.los.bank.Rbac.repository;

import com.los.bank.Rbac.entity.RolePermission;
import com.los.bank.Rbac.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByIdRoleId(Integer roleId);
}
