package com.arriyiaconsulting.siasaleo.service.security.identity.boundary;

import com.arriyiaconsulting.siasaleo.service.security.identity.control.RegistrationService;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.RegisterRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.RegistrationResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.ResendCodeRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.ResendResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.UserAccountDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.VerificationResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.VerifyRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * Sign-up endpoints. Each handler is an exhaustive switch over the service's
 * sealed result type, so every business outcome has an explicit HTTP mapping.
 *
 * Marked @PermitAll rather than left to RolesAllowedFeature's open default:
 * these are the endpoints that mint the very token authorization needs, so
 * they must stay reachable even if that default is ever tightened.
 */
@Path("auth")
@RequestScoped
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegistrationResource {

    @Inject
    private RegistrationService service;

    @Inject
    private UserAccountRepository accounts;

    @POST
    @Path("register")
    public Response register(@Valid RegisterRequest request, @Context UriInfo uriInfo) {
        return switch (service.register(request)) {
            case RegistrationResult.Registered(UserAccountDto account) ->
                    Response.created(uriInfo.getBaseUriBuilder()
                                    .path("auth/accounts/{id}").build(account.id()))
                            .entity(account)
                            .build();
            case RegistrationResult.InvalidIdentifier(String reason) -> error(Status.BAD_REQUEST, reason);
            case RegistrationResult.PasswordMismatch() ->
                    error(Status.BAD_REQUEST, "Password and confirmation do not match");
            case RegistrationResult.WeakPassword(String reason) -> error(Status.BAD_REQUEST, reason);
            case RegistrationResult.IdentifierTaken() ->
                    error(Status.CONFLICT, "An account with this email or phone already exists");
        };
    }

    @POST
    @Path("verify")
    public Response verify(@Valid VerifyRequest request) {
        return switch (service.verify(request)) {
            case VerificationResult.Verified(UserAccountDto account) -> Response.ok(account).build();
            case VerificationResult.AccountNotFound() ->
                    error(Status.NOT_FOUND, "No pending account for this email or phone");
            case VerificationResult.AlreadyVerified() ->
                    error(Status.CONFLICT, "Account is already verified");
            case VerificationResult.AccountUnavailable() ->
                    error(Status.FORBIDDEN, "Account is locked or disabled");
            case VerificationResult.CodeExpired() ->
                    error(Status.GONE, "Verification code expired; request a new one");
            case VerificationResult.CodeInvalid(int attemptsRemaining) ->
                    error(Status.BAD_REQUEST, "Incorrect code; " + attemptsRemaining + " attempt(s) remaining");
            case VerificationResult.TooManyAttempts() ->
                    error(Status.TOO_MANY_REQUESTS, "Too many incorrect attempts; request a new code");
        };
    }

    @POST
    @Path("resend-code")
    public Response resendCode(@Valid ResendCodeRequest request) {
        return switch (service.resendCode(request)) {
            case ResendResult.CodeSent() ->
                    Response.accepted(Map.of("message", "Verification code sent")).build();
            case ResendResult.AccountNotFound() ->
                    error(Status.NOT_FOUND, "No pending account for this email or phone");
            case ResendResult.AlreadyVerified() ->
                    error(Status.CONFLICT, "Account is already verified");
            case ResendResult.AccountUnavailable() ->
                    error(Status.FORBIDDEN, "Account is locked or disabled");
        };
    }

    @GET
    @Path("accounts/{id}")
    public UserAccountDto account(@PathParam("id") Long id) {
        return accounts.findById(id).map(UserAccountDto::from)
                .orElseThrow(NotFoundException::new);
    }

    // Same error shape as IllegalArgumentExceptionMapper: {"error": "..."}.
    private static Response error(Status status, String message) {
        return Response.status(status).entity(Map.of("error", message)).build();
    }
}
