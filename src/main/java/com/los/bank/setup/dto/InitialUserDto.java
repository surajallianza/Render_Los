package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InitialUserDto {

    @JsonProperty("emp_id")
    @JsonAlias({"enp_id", "emp_no"})
    private String empId;

    private String username;
    private String title;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("last_name")
    private String lastName;

    private String dob;
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String mobile;
    private String gender;
    private String designation;

    @JsonProperty("user_role")
    private String userRole;

    @JsonProperty("system_role")
    private String systemRole;

    private String status;

    @JsonProperty("login_branch")
    @JsonAlias({"login_branch_id"})
    private Long loginBranch;

    @JsonProperty("branch_name")
    private String branchName;

    @JsonProperty("branch_code")
    private String branchCode;

    @JsonProperty("login_time")
    private String loginTime;

    @JsonProperty("logout_time")
    private String logoutTime;

    @JsonProperty("session_timeout")
    @JsonAlias({"inactive_session_timeout"})
    private Integer sessionTimeout;

    @JsonProperty("no_of_bad_logins")
    private Integer noOfBadLogins;

    public InitialUserDto() {
    }

    public InitialUserDto(String empId, String username, String title, String firstName, String middleName,
                          String lastName, String dob, String email, String password, String mobile,
                          String gender, String designation, String userRole, String systemRole,
                          String status, Long loginBranch, String loginTime, String logoutTime,
                          Integer sessionTimeout, Integer noOfBadLogins) {
        this.empId = empId;
        this.username = username;
        this.title = title;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dob = dob;
        this.email = email;
        this.password = password;
        this.mobile = mobile;
        this.gender = gender;
        this.designation = designation;
        this.userRole = userRole;
        this.systemRole = systemRole;
        this.status = status;
        this.loginBranch = loginBranch;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.sessionTimeout = sessionTimeout;
        this.noOfBadLogins = noOfBadLogins;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String empId;
        private String username;
        private String title;
        private String firstName;
        private String middleName;
        private String lastName;
        private String dob;
        private String email;
        private String password;
        private String mobile;
        private String gender;
        private String designation;
        private String userRole;
        private String systemRole;
        private String status;
        private Long loginBranch;
        private String branchName;
        private String branchCode;
        private String loginTime;
        private String logoutTime;
        private Integer sessionTimeout;
        private Integer noOfBadLogins;

        public Builder empId(String empId) { this.empId = empId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder middleName(String middleName) { this.middleName = middleName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder dob(String dob) { this.dob = dob; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder mobile(String mobile) { this.mobile = mobile; return this; }
        public Builder gender(String gender) { this.gender = gender; return this; }
        public Builder designation(String designation) { this.designation = designation; return this; }
        public Builder userRole(String userRole) { this.userRole = userRole; return this; }
        public Builder systemRole(String systemRole) { this.systemRole = systemRole; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder loginBranch(Long loginBranch) { this.loginBranch = loginBranch; return this; }
        public Builder branchName(String branchName) { this.branchName = branchName; return this; }
        public Builder branchCode(String branchCode) { this.branchCode = branchCode; return this; }
        public Builder loginTime(String loginTime) { this.loginTime = loginTime; return this; }
        public Builder logoutTime(String logoutTime) { this.logoutTime = logoutTime; return this; }
        public Builder sessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; return this; }
        public Builder noOfBadLogins(Integer noOfBadLogins) { this.noOfBadLogins = noOfBadLogins; return this; }

        public InitialUserDto build() {
            InitialUserDto dto = new InitialUserDto(empId, username, title, firstName, middleName, lastName, dob, email,
                    password, mobile, gender, designation, userRole, systemRole, status, loginBranch,
                    loginTime, logoutTime, sessionTimeout, noOfBadLogins);
            dto.setBranchName(branchName);
            dto.setBranchCode(branchCode);
            return dto;
        }
    }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getSystemRole() { return systemRole; }
    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getLoginBranch() { return loginBranch; }
    public void setLoginBranch(Long loginBranch) { this.loginBranch = loginBranch; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getLoginTime() { return loginTime; }
    public void setLoginTime(String loginTime) { this.loginTime = loginTime; }

    public String getLogoutTime() { return logoutTime; }
    public void setLogoutTime(String logoutTime) { this.logoutTime = logoutTime; }

    public Integer getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    public Integer getNoOfBadLogins() { return noOfBadLogins; }
    public void setNoOfBadLogins(Integer noOfBadLogins) { this.noOfBadLogins = noOfBadLogins; }

    public String getEffectiveEmpNo() {
        if (empId != null && !empId.trim().isEmpty()) {
            return empId.trim();
        }
        return "EMP001";
    }

    public String getEffectiveUsername() {
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "superadmin";
    }

    public String getEffectiveFirstName() {
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim();
        }
        return "Super";
    }

    public String getEffectiveLastName() {
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim();
        }
        return "Admin";
    }

    public String getEffectiveFullName() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.trim().isEmpty()) {
            sb.append(title.trim()).append(" ");
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }
        return sb.length() > 0 ? sb.toString().trim() : getEffectiveUsername();
    }

    public String getEffectiveEmail() {
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        return getEffectiveUsername() + "@bank.com";
    }

    public String getEffectiveUserRole() {
        if (userRole != null && !userRole.trim().isEmpty()) {
            return userRole.trim().toUpperCase();
        }
        return "SUPER_ADMIN";
    }
}
