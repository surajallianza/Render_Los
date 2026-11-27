package com.los.bank.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.los.bank.security.validation.ValidPasswordBytes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCreateRequest {

    @JsonProperty("bank_code")
    @JsonAlias({"bankCode", "bank_name", "bankName", "database_name", "databaseName"})
    private String bankCode;

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

    @JsonProperty("system_role")
    @JsonAlias({"systemRole"})
    private Integer systemRole;

    @JsonProperty("status")
    private String status;

    @JsonProperty("login_branch_id")
    @JsonAlias({"login_branch", "loginBranchId", "loginBranch"})
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

    @JsonProperty("generate_token")
    @JsonAlias({"generateToken"})
    @Builder.Default
    private Boolean generateToken = true;
}
