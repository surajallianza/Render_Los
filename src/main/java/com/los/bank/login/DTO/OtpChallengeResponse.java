package com.los.bank.login.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpChallengeResponse {
    private Boolean twoFactorRequired;
    private String tempToken;
    private String maskedEmail;
    private String message;
}
