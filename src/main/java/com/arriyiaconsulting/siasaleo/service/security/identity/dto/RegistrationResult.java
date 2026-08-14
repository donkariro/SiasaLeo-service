package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

/**
 * Every way a sign-up attempt can end. The boundary switches over this
 * exhaustively, so adding a variant is a compile error until every caller
 * handles it.
 */
public sealed interface RegistrationResult {

    record Registered(UserAccountDto account) implements RegistrationResult {
    }

    record InvalidIdentifier(String reason) implements RegistrationResult {
    }

    record PasswordMismatch() implements RegistrationResult {
    }

    record WeakPassword(String reason) implements RegistrationResult {
    }

    record IdentifierTaken() implements RegistrationResult {
    }
}
