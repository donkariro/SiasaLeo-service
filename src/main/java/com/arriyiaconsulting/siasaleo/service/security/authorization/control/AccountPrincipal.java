package com.arriyiaconsulting.siasaleo.service.security.authorization.control;

import java.security.Principal;
import java.util.Set;

/**
 * The authenticated caller, built from a verified token. getName returns the
 * upn — the account's normalized email or phone — because that is what
 * Principal means elsewhere, but accountId is what the application should use:
 * it is the token's sub claim, and unlike the contact point it cannot collide
 * or change.
 */
public record AccountPrincipal(Long accountId, String upn, Set<String> roles)
        implements Principal {

    @Override
    public String getName() {
        return upn != null ? upn : String.valueOf(accountId);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
