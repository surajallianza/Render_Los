package com.los.bank.rule.dto;

import lombok.*;

/**
 * Body of PATCH /api/rules/{id}/status: { "status": "ACTIVE" | "INACTIVE" }.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleStatusRequest {
    private String status;
}
