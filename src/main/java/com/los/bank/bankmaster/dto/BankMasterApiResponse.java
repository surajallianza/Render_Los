package com.los.bank.bankmaster.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper for Bank Master module endpoints.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BankMasterApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public BankMasterApiResponse() {
    }

    public BankMasterApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> BankMasterApiResponse<T> success(String message, T data) {
        return new BankMasterApiResponse<>(true, message, data);
    }

    public static <T> BankMasterApiResponse<T> error(String message) {
        return new BankMasterApiResponse<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
