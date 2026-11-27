package com.los.bank.interestrate.dto;

import lombok.*;

/**
 * Body of the reject call (reason is required there). Approve takes no body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateReviewRequest {
    private String reason;
}
