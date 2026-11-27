package com.los.bank.login.service;

import com.los.bank.login.entity.UserJwtSession;
import com.los.bank.login.repository.UserJwtSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final long tempTokenValidityMs;
    private final UserJwtSessionRepository jwtSessionRepository;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:BankLosSecretKeyForJwtSigningMustBeAtLeast256BitsLongAndSecure2026!}") String secret,
            @Value("${app.jwt.access-token-validity-seconds:900}") long accessTokenValiditySeconds,
            @Value("${app.jwt.refresh-token-validity-seconds:604800}") long refreshTokenValiditySeconds,
            UserJwtSessionRepository jwtSessionRepository) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValiditySeconds * 1000L;
        this.refreshTokenValidityMs = refreshTokenValiditySeconds * 1000L;
        this.tempTokenValidityMs = 5 * 60 * 1000L; // 5 minutes for 2FA OTP
        this.jwtSessionRepository = jwtSessionRepository;
    }

    /**
     * Generates a temporary signed token for 2FA challenge stage.
     */
    public String generateTempToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tempTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("stage", "2FA_PENDING")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates an Access Token containing dynamic user role and permissions.
     */
    public String generateAccessToken(Long userId, String email, String roleName, Integer roleId, Long branchId, List<String> permissions, String jti) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", roleName)
                .claim("role_id", roleId)
                .claim("branch_id", branchId)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a Refresh Token.
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT token. Returns Claims if valid.
     */
    public Claims parseAndValidateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("[JwtTokenProvider] Invalid or expired token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifies that the JTI session is active and not invalidated in u008.
     */
    public boolean isSessionActive(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return true;
        }
        Optional<UserJwtSession> sessionOpt = jwtSessionRepository.findByJtiAndInvalidatedFalse(jti);
        return sessionOpt.isPresent();
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMs / 1000L;
    }
}
