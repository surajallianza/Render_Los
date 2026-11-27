package com.los.bank.loanEligibility.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EligibilityApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Map<String, String> errors;

    public static <T> EligibilityApiResponse<T> success(String message, T data) {
        return EligibilityApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> EligibilityApiResponse<T> error(String message, Map<String, String> errors) {
        return EligibilityApiResponse.<T>builder().success(false).message(message).errors(errors).build();
    }
}
