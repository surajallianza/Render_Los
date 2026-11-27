package com.los.bank.login.DTO;

import com.los.bank.security.validation.ValidPasswordBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Login Request DTO for Internal Bank Staff.
 * Strictly uses email and password only.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @ValidPasswordBytes(message = "Password must not exceed 72 bytes (in UTF-8 encoding)")
    private String password;
}
