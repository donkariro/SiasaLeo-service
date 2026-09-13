package com.arriyiaconsulting.siasaleo.service.security.authorization.control;

import jakarta.ws.rs.core.SecurityContext;

/**
 * The SecurityContext a verified bearer token installs for the rest of the
 * request. Roles come from the token's groups claim, so isUserInRole never
 * touches the database.
 */
public class TokenSecurityContext implements SecurityContext {

    public static final String BEARER = "Bearer";

    private final AccountPrincipal principal;
    private final boolean secure;

    public TokenSecurityContext(AccountPrincipal principal, boolean secure) {
        this.principal = principal;
        this.secure = secure;
    }

    @Override
    public AccountPrincipal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return BEARER;
    }
}
