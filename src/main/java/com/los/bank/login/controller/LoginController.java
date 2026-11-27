package com.los.bank.login.controller;

import com.los.bank.login.DTO.*;
import com.los.bank.login.service.LoginService;
import com.los.bank.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * Phase 1: Internal Staff Login (Email & Password).
     * Dispatches mandatory 2FA OTP and returns challenge token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> login(@Valid @RequestBody LoginRequest request) {
        OtpChallengeResponse response = loginService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Credentials verified. Two-factor authentication required.", response));
    }

    /**
     * Phase 2: Verifies submitted 2FA OTP code and returns Access and Refresh Tokens.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = loginService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * Refreshes expired Access Token using valid Refresh Token.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = loginService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Logs out user and revokes active JWT session.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        loginService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
