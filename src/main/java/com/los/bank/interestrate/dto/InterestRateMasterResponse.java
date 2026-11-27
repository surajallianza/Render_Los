package com.los.bank.interestrate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterestRateMasterResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    /** PENDING, APPROVED or REJECTED. */
    private String status;

    /** True for the APPROVED sheet that new loans of this product use today. */
    private boolean currentlyApplied;

    /** True while the sheet can still be edited or deleted (not yet in effect). */
    private boolean editable;

    private String remarks;
    private String rejectionReason;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long modifiedBy;
    private LocalDateTime updatedAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    private List<InterestRateTierResponse> tiers;

    /**
     * Non-blocking hints, for example loan amounts or tenures of the product that no tier covers.
     */
    private List<String> warnings;
}
