package com.los.bank.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clean User API response.
 * Strictly adheres to security rules: NEVER includes password or password_hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("emp_no")
    private String empNo;

    @JsonProperty("username")
    private String username;

    @JsonProperty("title")
    private String title;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("mobile")
    private String mobile;

    @JsonProperty("dob")
    private String dob;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("designation")
    private String designation;

    @JsonProperty("role_id")
    private Integer roleId;

    @JsonProperty("user_role")
    private String userRole;

    @JsonProperty("system_role")
    private Integer systemRole;

    @JsonProperty("login_branch_id")
    private Long loginBranchId;

    @JsonProperty("login_time")
    private String loginTime;

    @JsonProperty("logout_time")
    private String logoutTime;

    @JsonProperty("inactive_session_timeout")
    private Integer sessionTimeout;

    @JsonProperty("status")
    private String status;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("no_of_bad_logins")
    private Integer noOfBadLogins;

    @JsonProperty("last_login_date")
    private String lastLoginDate;

    @JsonProperty("last_login_time")
    private String lastLoginTime;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("verified_by")
    private Long verifiedBy;

    @JsonProperty("verified_date")
    private String verifiedDate;

    @JsonProperty("modified_by")
    private Long modifiedBy;

    /**
     * Session/auth token returned upon creation or token generation.
     * Contains no password information.
     */
    @JsonProperty("token")
    private String token;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
