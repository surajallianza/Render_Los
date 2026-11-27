package com.los.bank.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity mapping table u004 (User-Branch mapping).
 */
@Entity
@Table(name = "u004")
@IdClass(UserBranchId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranchMapping {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "title", nullable = false, length = 20)
    private String title;
}
