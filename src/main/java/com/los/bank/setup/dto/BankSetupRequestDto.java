package com.los.bank.setup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankSetupRequestDto {

    private BankDto bank;

    @JsonProperty("branch")
    @JsonAlias({"branch_information", "branch_details"})
    private BranchDto branch;

    @JsonProperty("initial_user")
    private InitialUserDto initialUser;
}

