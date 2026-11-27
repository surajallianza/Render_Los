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

import java.time.LocalDate;

/**
 * Entity mapping table u002 (User personal profile and role mapping).
 */
@Entity
@Table(name = "u002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "role_id")
    private Integer roleId;
}
