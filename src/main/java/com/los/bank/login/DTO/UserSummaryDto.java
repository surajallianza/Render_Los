package com.los.bank.login.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    private Long id;
    private String empNo;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String designation;
    private Integer roleId;
    private String roleName;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private List<String> permissions;
}
