package com.arriyiaconsulting.siasaleo.service.security.authorization.boundary;

import com.arriyiaconsulting.siasaleo.service.security.authorization.control.AccountPrincipal;
import com.arriyiaconsulting.siasaleo.service.security.authorization.control.TokenSecurityContext;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.TokenIssuer;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.TokenVerification;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns a bearer token into the request's SecurityContext, ahead of any
 * authorization check (Priorities.AUTHENTICATION).
 *
 * A request with no Authorization header is left anonymous rather than
 * rejected: whether the endpoint needs a caller is RolesAllowedFeature's
 * decision, and the reference endpoints that make up most of this API are
 * readable without one. A header that is present but does not verify is
 * refused here and now — a caller who sent a token meant it to be used, so
 * carrying on as anonymous would answer them under an identity they did not
 * ask for.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TokenAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER =
            Logger.getLogger(TokenAuthenticationFilter.class.getName());

    private static final String BEARER_PREFIX = "bearer ";

    @Inject
    private TokenIssuer tokens;

    @Override
    public void filter(ContainerRequestContext request) {
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX)) {
            return;
        }

        switch (tokens.verify(header.substring(BEARER_PREFIX.length()).trim())) {
            case TokenVerification.Valid(TokenVerification.VerifiedToken token) ->
                    request.setSecurityContext(new TokenSecurityContext(
                            new AccountPrincipal(token.accountId(), token.upn(), token.roles()),
                            request.getUriInfo().getRequestUri().getScheme().equals("https")));
            case TokenVerification.Expired() ->
                    abort(request, "Token has expired; log in again");
            // The reason names how a forgery failed, which is useful in the
            // log and useful to an attacker, so it goes only to the log.
            case TokenVerification.Invalid(String reason) -> {
                LOGGER.log(Level.FINE, "Rejected bearer token: {0}", reason);
                abort(request, "Invalid token");
            }
        }
    }

    private static void abort(ContainerRequestContext request, String message) {
        request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, TokenSecurityContext.BEARER)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", message))
                .build());
    }
}
