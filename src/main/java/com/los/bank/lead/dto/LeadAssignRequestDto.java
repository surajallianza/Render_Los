package com.los.bank.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for assigning a lead to an employee.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadAssignRequestDto {

    @JsonProperty("employeeId")
    @JsonAlias({"employee_id", "empId", "emp_id", "assignedUserId", "assigned_user_id"})
    private String employeeId;

    @JsonProperty("employeeName")
    @JsonAlias({"employee_name", "empName", "emp_name"})
    private String employeeName;

    @JsonProperty("assignedBy")
    @JsonAlias({"assigned_by"})
    private String assignedBy;

    @JsonProperty("notes")
    private String notes;
}
