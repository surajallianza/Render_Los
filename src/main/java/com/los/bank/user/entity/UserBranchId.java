package com.los.bank.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBranchId implements Serializable {
    private Long userId;
    private Long branchId;
}
