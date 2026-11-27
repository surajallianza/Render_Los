package com.los.bank.rule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response envelope for Rule Engine module endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RuleApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> RuleApiResponse<T> success(String message, T data) {
        return RuleApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> RuleApiResponse<T> success(T data) {
        return RuleApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> RuleApiResponse<T> error(String message) {
        return RuleApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
