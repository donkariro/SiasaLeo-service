package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

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
}
