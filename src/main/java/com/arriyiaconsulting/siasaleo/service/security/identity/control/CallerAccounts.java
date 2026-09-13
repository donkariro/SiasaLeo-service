package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.authorization.control.AccountPrincipal;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Resolves the account behind the current request, so a boundary never has to
 * take an account or person id from the client. Any endpoint that acts "as
 * the caller" — declaring a role, claiming a person — must go through here:
 * an id read from a path or body is an authorization decision handed to the
 * requester.
 *
 * TokenAuthenticationFilter installs an AccountPrincipal carrying the token's
 * sub claim, so the account id is already known and requireId needs no
 * database read at all. This resolves at most once per request.
 */
@RequestScoped
public class CallerAccounts {

    @Inject
    private UserAccountRepository accounts;

    private UserAccount resolved;

    public UserAccount require(SecurityContext securityContext) {
        if (resolved != null) {
            return resolved;
        }
        resolved = accounts.findById(requireId(securityContext))
                .orElseThrow(() -> new NotAuthorizedException("Bearer"));
        return resolved;
    }

    /** The caller's account id, straight from the token. */
    public Long requireId(SecurityContext securityContext) {
        Principal principal =
                securityContext == null ? null : securityContext.getUserPrincipal();
        if (principal instanceof AccountPrincipal account) {
            return account.accountId();
        }
        throw new NotAuthorizedException("Bearer");
    }
}
