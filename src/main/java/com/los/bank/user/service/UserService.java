package com.los.bank.user.service;

import com.los.bank.user.dto.UserCreateRequest;
import com.los.bank.user.dto.UserResponse;
import com.los.bank.user.dto.UserUpdateRequest;
import com.los.bank.user.entity.UserToken;

import java.util.List;

public interface UserService {

    /**
     * Retrieves all users with profile and security details.
     * Guaranteed fast performance with joined fetching.
     * Password information is strictly excluded from responses.
     */
    List<UserResponse> getAllUsers();

    /**
     * Creates a new user with BCrypt password encoding, password confirmation check,
     * profile record, security setting, optional branch mapping, and saves an authentication token.
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * Updates user profile and optional password.
     * If a new password is provided, verifies confirmation match and updates hash.
     * If omitted, existing password remains unchanged.
     */
    UserResponse updateUser(Long id, UserUpdateRequest request);

    /**
     * Retrieves a single user by primary ID without exposing passwords.
     */
    UserResponse getUserById(Long id);

    /**
     * Generates or records a token for the user in table u007.
     */
    UserToken saveToken(Long userId, String tokenValue, int expirationHours);
}
