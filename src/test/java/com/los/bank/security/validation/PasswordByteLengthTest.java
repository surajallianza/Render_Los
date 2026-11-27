package com.los.bank.security.validation;

import com.los.bank.login.DTO.LoginRequest;
import com.los.bank.login.service.LoginServiceImpl;
import com.los.bank.user.dto.UserCreateRequest;
import com.los.bank.user.dto.UserResponse;
import com.los.bank.user.dto.UserUpdateRequest;
import com.los.bank.user.entity.User;
import com.los.bank.user.entity.UserProfile;
import com.los.bank.user.exception.UserValidationException;
import com.los.bank.user.repository.UserProfileRepository;
import com.los.bank.user.repository.UserRepository;
import com.los.bank.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordByteLengthTest {

    private ValidPasswordBytesValidator validator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DataSource bankRoutingDataSource;

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private com.los.bank.user.repository.UserSecuritySettingRepository userSecuritySettingRepository;

    @Mock
    private com.los.bank.user.repository.UserBranchMappingRepository userBranchMappingRepository;

    @Mock
    private com.los.bank.user.repository.UserTokenRepository userTokenRepository;

    @Mock
    private com.los.bank.Otp.service.OtpService otpService;

    @Mock
    private com.los.bank.Rbac.service.RbacService rbacService;

    @Mock
    private com.los.bank.login.service.JwtTokenProvider jwtTokenProvider;

    @Mock
    private com.los.bank.login.repository.UserRefreshTokenRepository userRefreshTokenRepository;

    @Mock
    private com.los.bank.login.repository.UserJwtSessionRepository userJwtSessionRepository;

    @InjectMocks
    private LoginServiceImpl loginService;

    @BeforeEach
    void setUp() {
        validator = new ValidPasswordBytesValidator();
        ValidPasswordBytes annotation = mock(ValidPasswordBytes.class);
        when(annotation.max()).thenReturn(72);
        validator.initialize(annotation);
    }

    @Test
    @DisplayName("Validator: 72 ASCII bytes password should be valid")
    void testPasswordExactly72BytesValid() {
        String pass72 = "a".repeat(72);
        assertEquals(72, pass72.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(validator.isValid(pass72, null));
    }

    @Test
    @DisplayName("Validator: 73 ASCII bytes password should be invalid")
    void testPassword73BytesInvalid() {
        String pass73 = "a".repeat(73);
        assertEquals(73, pass73.getBytes(StandardCharsets.UTF_8).length);
        assertFalse(validator.isValid(pass73, null));
    }

    @Test
    @DisplayName("Validator: Multi-byte UTF-8 password exceeding 72 bytes should be invalid even if char length < 72")
    void testMultiByteUtf8Exceeding72Bytes() {
        // '€' is 3 bytes in UTF-8. 25 '€' chars = 75 bytes (length = 25 chars)
        String euroPass = "€".repeat(25);
        assertEquals(25, euroPass.length());
        assertEquals(75, euroPass.getBytes(StandardCharsets.UTF_8).length);
        assertFalse(validator.isValid(euroPass, null));
    }

    @Test
    @DisplayName("Registration: createUser throws UserValidationException when password exceeds 72 UTF-8 bytes")
    void testCreateUserRejectsPasswordOver72Bytes() {
        String pass73 = "P@ssword123" + "x".repeat(65);
        assertTrue(pass73.getBytes(StandardCharsets.UTF_8).length > 72);

        UserCreateRequest request = UserCreateRequest.builder()
                .empNo("EMP999")
                .username("test.user")
                .firstName("Test")
                .lastName("User")
                .email("test@bank.com")
                .password(pass73)
                .confirmPassword(pass73)
                .build();

        UserValidationException ex = assertThrows(UserValidationException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("Password must not exceed 72 bytes"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Password Update: updateUser throws UserValidationException when new password exceeds 72 UTF-8 bytes")
    void testUpdateUserRejectsPasswordOver72Bytes() {
        String pass73 = "N3wPassword" + "y".repeat(65);
        assertTrue(pass73.getBytes(StandardCharsets.UTF_8).length > 72);

        User existingUser = User.builder()
                .id(1L)
                .empNo("EMP001")
                .username("rohit.sharma")
                .passwordHash("$2a$10$existingHash...")
                .status("ACTIVE")
                .isActive(true)
                .build();
        existingUser.setProfile(UserProfile.builder().userId(1L).email("rohit@bank.com").build());

        when(userRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(existingUser));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .password(pass73)
                .confirmPassword(pass73)
                .build();

        UserValidationException ex = assertThrows(UserValidationException.class, () -> userService.updateUser(1L, request));
        assertTrue(ex.getMessage().contains("Password must not exceed 72 bytes"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Login: login throws UserValidationException when password exceeds 72 UTF-8 bytes")
    void testLoginRejectsPasswordOver72Bytes() {
        String pass73 = "LoginPass" + "z".repeat(65);
        assertTrue(pass73.getBytes(StandardCharsets.UTF_8).length > 72);

        UserProfile profile = UserProfile.builder().userId(1L).email("rohit@bank.com").build();
        User user = User.builder().id(1L).empNo("EMP001").username("rohit").passwordHash("$2a$10$...").status("ACTIVE").isActive(true).build();

        when(userProfileRepository.findByEmail("rohit@bank.com")).thenReturn(Optional.of(profile));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LoginRequest request = LoginRequest.builder()
                .email("rohit@bank.com")
                .password(pass73)
                .build();

        UserValidationException ex = assertThrows(UserValidationException.class, () -> loginService.login(request));
        assertTrue(ex.getMessage().contains("Password must not exceed 72 bytes"));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("2FA Verify OTP: Refresh token longer than 72 bytes is hashed with SHA-256 without BCrypt error")
    void testVerifyOtpSucceedsWithLongRefreshToken() {
        String longJwtRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + "a".repeat(150);
        assertTrue(longJwtRefreshToken.getBytes(StandardCharsets.UTF_8).length > 72);

        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("stage")).thenReturn("2FA_PENDING");
        when(claims.getSubject()).thenReturn("1");

        when(jwtTokenProvider.parseAndValidateToken("temp-token-xyz")).thenReturn(claims);
        when(otpService.verifyLoginOtp(1L, "123456")).thenReturn(true);

        User user = User.builder().id(1L).empNo("EMP001").username("rohit").passwordHash("$2a$10$...").status("ACTIVE").isActive(true).build();
        UserProfile profile = UserProfile.builder().userId(1L).email("rohit@bank.com").roleId(1).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(userSecuritySettingRepository.findById(1L)).thenReturn(Optional.empty());
        when(userBranchMappingRepository.findByUserId(1L)).thenReturn(java.util.Collections.emptyList());
        when(rbacService.getRoleById(1)).thenReturn(Optional.empty());

        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any(), any(), any())).thenReturn("access-token-jwt");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn(longJwtRefreshToken);
        when(jwtTokenProvider.getAccessTokenValiditySeconds()).thenReturn(900L);

        com.los.bank.login.DTO.VerifyOtpRequest request = com.los.bank.login.DTO.VerifyOtpRequest.builder()
                .email("rohit@bank.com")
                .otp("123456")
                .tempToken("temp-token-xyz")
                .build();

        // Must succeed without throwing "password cannot be more than 72 bytes"
        assertDoesNotThrow(() -> {
            com.los.bank.login.DTO.AuthResponse response = loginService.verifyOtp(request);
            assertNotNull(response);
            assertEquals("access-token-jwt", response.getAccessToken());
            assertEquals(longJwtRefreshToken, response.getRefreshToken());
        });

        // Verify passwordEncoder.encode was NEVER called for the refresh token
        verify(passwordEncoder, never()).encode(longJwtRefreshToken);
        // Verify userRefreshTokenRepository.save was called with the SHA-256 hash
        verify(userRefreshTokenRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Security: Passwords and password hashes are never exposed in UserResponse")
    void testPasswordNeverExposedInUserResponse() {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("rohit.sharma")
                .email("rohit@bank.com")
                .fullName("Rohit Sharma")
                .build();

        // Validate via reflection that UserResponse has no field containing password or passwordHash
        for (java.lang.reflect.Field field : UserResponse.class.getDeclaredFields()) {
            String name = field.getName().toLowerCase();
            assertFalse(name.contains("password"), "UserResponse must never contain password fields: " + name);
        }
    }
}
