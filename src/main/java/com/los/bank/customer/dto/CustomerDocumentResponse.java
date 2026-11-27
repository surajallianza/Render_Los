package com.los.bank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.los.bank.lookup.dto.LookupValue;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDocumentResponse {

    private Long id;
    private String customerCode;
    private LookupValue documentType;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private String remarks;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;
    /** IDs of older documents this upload replaced (soft deleted). Empty when nothing was replaced. */
    private java.util.List<Long> replacedDocumentIds;
}
