package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

/** Every way a verification attempt can end. */
public sealed interface VerificationResult {

    record Verified(UserAccountDto account) implements VerificationResult {
    }

    record AccountNotFound() implements VerificationResult {
    }

    record AlreadyVerified() implements VerificationResult {
    }

    /** The account is LOCKED or DISABLED; verification cannot proceed. */
    record AccountUnavailable() implements VerificationResult {
    }

    /** Also covers "no active code at all" — the remedy (request a new code) is the same. */
    record CodeExpired() implements VerificationResult {
    }

    record CodeInvalid(int attemptsRemaining) implements VerificationResult {
    }

    record TooManyAttempts() implements VerificationResult {
    }
}
