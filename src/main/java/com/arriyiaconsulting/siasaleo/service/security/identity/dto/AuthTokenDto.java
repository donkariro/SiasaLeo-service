package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import java.time.OffsetDateTime;

/**
 * Successful login response: the bearer token for the Authorization header
 * plus the account it authenticates.
 */
public record AuthTokenDto(
        String tokenType,
        String accessToken,
        OffsetDateTime expiresAt,
        UserAccountDto account) {

    public static AuthTokenDto bearer(String accessToken, OffsetDateTime expiresAt,
            UserAccountDto account) {
        return new AuthTokenDto("Bearer", accessToken, expiresAt, account);
    }
}
