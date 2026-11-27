package com.los.bank.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity mapping table u003 (System role, login timing, branch, and session constraints).
 */
@Entity
@Table(name = "u003")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSecuritySetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "system_role", nullable = false)
    @Builder.Default
    private Integer systemRole = 1;

    @Column(name = "login_branch_id")
    private Long loginBranchId;

    @Column(name = "login_time")
    private LocalTime loginTime;

    @Column(name = "logout_time")
    private LocalTime logoutTime;

    @Column(name = "inactive_session_timeout")
    @Builder.Default
    private Integer inactiveSessionTimeout = 30;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
