package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankSetupDataDto {
    private Long bankId;
    private String bankName;
    private String bankCode;
    private String databaseName;
    private boolean databaseCreated;
    private boolean migrationCompleted;
    private boolean superAdminCreated;
    private boolean setupCompleted;

    private BankDto bank;

    @JsonProperty("branch")
    private BranchDto branch;

    @JsonProperty("initial_user")
    private InitialUserDto initialUser;
}
