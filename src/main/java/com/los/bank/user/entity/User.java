package com.los.bank.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping table u001 (Core User credentials and access status).
 */
@Entity
@Table(name = "u001")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_no", nullable = false, unique = true, length = 30)
    private String empNo;

    @Column(name = "username", nullable = false, unique = true, length = 80)
    private String username;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "no_of_bad_logins", nullable = false)
    @Builder.Default
    private Integer noOfBadLogins = 0;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Column(name = "last_login_time")
    private LocalTime lastLoginTime;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserProfile profile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserSecuritySetting securitySetting;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<UserToken> tokens = new ArrayList<>();

    public void setProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    public void setSecuritySetting(UserSecuritySetting securitySetting) {
        this.securitySetting = securitySetting;
        if (securitySetting != null) {
            securitySetting.setUser(this);
        }
    }

    public void addToken(UserToken token) {
        if (tokens == null) {
            tokens = new ArrayList<>();
        }
        tokens.add(token);
        token.setUser(this);
        token.setUserId(this.id);
    }
}
