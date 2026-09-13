package com.arriyiaconsulting.siasaleo.service.security.identity.boundary;

import com.arriyiaconsulting.siasaleo.service.security.identity.control.AuthenticationService;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.AuthTokenDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.LoginRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.LoginResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.UserAccountDto;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Login endpoint, an exhaustive switch over LoginResult so every business
 * outcome has an explicit HTTP mapping. Lives beside RegistrationResource
 * under the same /auth root; the sub-paths do not overlap.
 *
 * @PermitAll for the same reason as RegistrationResource: this is where a
 * token comes from, so it can never require one.
 */
@Path("auth")
@RequestScoped
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    // 423 Locked has no constant in Response.Status.
    private static final int SC_LOCKED = 423;

    @Inject
    private AuthenticationService service;

    @POST
    @Path("login")
    public Response login(@Valid LoginRequest request) {
        return switch (service.login(request)) {
            case LoginResult.Success(UserAccountDto account, String token, OffsetDateTime expiresAt) ->
                    Response.ok(AuthTokenDto.bearer(token, expiresAt, account)).build();
            case LoginResult.InvalidCredentials() ->
                    error(Status.UNAUTHORIZED.getStatusCode(), "Invalid email/phone or password");
            case LoginResult.NotVerified() ->
                    error(Status.FORBIDDEN.getStatusCode(),
                            "Account is not verified yet; enter the code you were sent or request a new one");
            case LoginResult.TemporarilyLocked(OffsetDateTime until) ->
                    error(SC_LOCKED, "Too many failed attempts; try again after " + until);
            case LoginResult.AccountUnavailable() ->
                    error(Status.FORBIDDEN.getStatusCode(), "Account is locked or disabled");
        };
    }

    // Same error shape as IllegalArgumentExceptionMapper: {"error": "..."}.
    private static Response error(int status, String message) {
        return Response.status(status).entity(Map.of("error", message)).build();
    }
}
