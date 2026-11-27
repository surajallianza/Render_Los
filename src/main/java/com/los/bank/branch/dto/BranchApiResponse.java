package com.los.bank.branch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper for Branch Management endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BranchApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> BranchApiResponse<T> success(String message, T data) {
        return BranchApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BranchApiResponse<T> error(String message) {
        return BranchApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
