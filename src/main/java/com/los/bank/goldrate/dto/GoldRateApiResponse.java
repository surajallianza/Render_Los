package com.los.bank.goldrate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper for Gold Rate Master & Audit Trail endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class GoldRateApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> GoldRateApiResponse<T> success(String message, T data) {
        return GoldRateApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> GoldRateApiResponse<T> success(T data) {
        return GoldRateApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> GoldRateApiResponse<T> error(String message) {
        return GoldRateApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
