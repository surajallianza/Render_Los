package com.los.bank.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response envelope for Loan Product Module endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class LoanProductApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> LoanProductApiResponse<T> success(String message, T data) {
        return LoanProductApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> LoanProductApiResponse<T> success(T data) {
        return LoanProductApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> LoanProductApiResponse<T> error(String message) {
        return LoanProductApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
