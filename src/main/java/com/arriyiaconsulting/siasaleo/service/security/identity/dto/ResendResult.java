package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import java.time.OffsetDateTime;

/** Every way a resend-code request can end. */
public sealed interface ResendResult {

    record CodeSent() implements ResendResult {
    }

    record AccountNotFound() implements ResendResult {
    }

    record AlreadyVerified() implements ResendResult {
    }

    /** The account is LOCKED or DISABLED; no code will be sent. */
    record AccountUnavailable() implements ResendResult {
    }

    /** A verification code was issued recently; wait until retryAfter before resending. */
    record RateLimited(OffsetDateTime retryAfter) implements ResendResult {
    }
}
