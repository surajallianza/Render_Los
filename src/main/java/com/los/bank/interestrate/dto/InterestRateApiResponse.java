package com.los.bank.interestrate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response envelope for the interest rate master APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterestRateApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    /** Field-level validation errors, keyed like "effectiveDate" or "tiers[1].normalRate". */
    private Map<String, String> errors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> InterestRateApiResponse<T> success(String message, T data) {
        return InterestRateApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> InterestRateApiResponse<T> error(String message, Map<String, String> errors) {
        return InterestRateApiResponse.<T>builder().success(false).message(message).errors(errors).build();
    }
}
