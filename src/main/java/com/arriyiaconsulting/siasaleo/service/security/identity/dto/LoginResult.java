package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import java.time.OffsetDateTime;

/**
 * Every way a login attempt can end. InvalidCredentials deliberately covers
 * unknown identifier, malformed identifier and wrong password alike, so the
 * endpoint cannot be used to probe which accounts exist.
 */
public sealed interface LoginResult {

    record Success(UserAccountDto account, String token, OffsetDateTime tokenExpiresAt)
            implements LoginResult {
    }

    record InvalidCredentials() implements LoginResult {
    }

    /** Correct password, but the contact point was never verified. */
    record NotVerified() implements LoginResult {
    }

    /** Too many wrong passwords; retry once the window has passed. */
    record TemporarilyLocked(OffsetDateTime until) implements LoginResult {
    }

    /** Administratively LOCKED or DISABLED. */
    record AccountUnavailable() implements LoginResult {
    }
}
