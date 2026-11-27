package com.los.bank.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.los.bank.security.validation.ValidPasswordBytes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Universal User update request DTO.
 * Supports updating any single field, multiple fields, or all fields together.
 * Omitted or null fields are preserved and not overwritten.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUpdateRequest {

    @JsonProperty("bank_code")
    @JsonAlias({"bankCode", "bank_name", "bankName", "database_name", "databaseName"})
    private String bankCode;

    // --- Core User Details (u001) ---
    @JsonProperty("emp_no")
    @JsonAlias({"emp_id", "empNo", "empId"})
    private String empNo;

    @JsonProperty("username")
    private String username;

    @ValidPasswordBytes(message = "Password must not exceed 72 bytes (in UTF-8 encoding)")
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ValidPasswordBytes(message = "Confirm password must not exceed 72 bytes (in UTF-8 encoding)")
    @JsonProperty(value = "confirm_password", access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias({"confirmPassword"})
    private String confirmPassword;

    @JsonProperty("status")
    private String status;

    @JsonProperty("is_active")
    @JsonAlias({"isActive"})
    private Boolean isActive;

    @JsonProperty("no_of_bad_logins")
    @JsonAlias({"noOfBadLogins"})
    private Integer noOfBadLogins;

    @JsonProperty("last_login_date")
    @JsonAlias({"lastLoginDate"})
    private String lastLoginDate;

    @JsonProperty("last_login_time")
    @JsonAlias({"lastLoginTime"})
    private String lastLoginTime;

    // --- User Profile Details (u002) ---
    @JsonProperty("title")
    private String title;

    @JsonProperty("first_name")
    @JsonAlias({"firstName"})
    private String firstName;

    @JsonProperty("middle_name")
    @JsonAlias({"middleName"})
    private String middleName;

    @JsonProperty("last_name")
    @JsonAlias({"lastName"})
    private String lastName;

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
    @JsonAlias({"roleId"})
    private Integer roleId;

    @JsonProperty("user_role")
    @JsonAlias({"userRole"})
    private String userRole;

    // --- Security & Session Constraints (u003) ---
    @JsonProperty("system_role")
    @JsonAlias({"systemRole"})
    private Integer systemRole;

    @JsonProperty("login_branch_id")
    @JsonAlias({"login_branch", "loginBranchId", "loginBranch", "branch_id", "branchId"})
    private Long loginBranchId;

    @JsonProperty("login_time")
    @JsonAlias({"loginTime"})
    private String loginTime;

    @JsonProperty("logout_time")
    @JsonAlias({"logoutTime"})
    private String logoutTime;

    @JsonProperty("inactive_session_timeout")
    @JsonAlias({"sessionTimeout", "inactiveSessionTimeout"})
    private Integer sessionTimeout;

    @JsonProperty("created_by")
    @JsonAlias({"createdBy"})
    private Long createdBy;

    @JsonProperty("verified_by")
    @JsonAlias({"verifiedBy"})
    private Long verifiedBy;

    @JsonProperty("verified_date")
    @JsonAlias({"verifiedDate"})
    private String verifiedDate;

    @JsonProperty("modified_by")
    @JsonAlias({"modifiedBy"})
    private Long modifiedBy;
}
