package com.los.bank.login.service;

import com.los.bank.login.DTO.*;

public interface LoginService {

    /**
     * Phase 1: Validates internal staff email and password.
     * Enforces 2FA and generates a one-time OTP stored in u005.
     */
    OtpChallengeResponse login(LoginRequest request);

    /**
     * Phase 2: Verifies submitted 2FA OTP, resolves dynamic roles and permissions,
     * tracks JTI in u008 and refresh token in u007, and returns complete AuthResponse.
     */
    AuthResponse verifyOtp(VerifyOtpRequest request);

    /**
     * Refreshes expired access token using a valid refresh token.
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logs out the user and invalidates the session in u008.
     */
    void logout(String authHeader);
}
