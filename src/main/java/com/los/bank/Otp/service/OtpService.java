package com.los.bank.Otp.service;

public interface OtpService {

    /**
     * Generates a cryptographically secure 6-digit OTP for 2FA login, hashes it,
     * stores it in u005 table with 5 minutes validity, and returns the plain OTP.
     */
    String generateLoginOtp(Long userId);

    /**
     * Verifies the provided OTP against the latest active record in u005 for the user.
     * Marks the record as used upon successful verification.
     */
    boolean verifyLoginOtp(Long userId, String plainOtp);
}
