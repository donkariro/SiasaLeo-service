package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Every way verifying a bearer token can end. Expired is kept apart from
 * Invalid because it is the one failure a well-behaved client can fix on its
 * own, by logging in again; the reason on Invalid is for the server log, not
 * for the response, since telling a caller why their forgery failed only helps
 * them forge a better one.
 */
public sealed interface TokenVerification {

    record Valid(VerifiedToken token) implements TokenVerification {
    }

    record Expired() implements TokenVerification {
    }

    record Invalid(String reason) implements TokenVerification {
    }

    /** The claims this service trusts once the signature checks out. */
    record VerifiedToken(Long accountId, String upn, Set<String> roles,
                         OffsetDateTime expiresAt) {
    }
}
