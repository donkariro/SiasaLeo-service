package com.arriyiaconsulting.siasaleo.service.domain.party.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonClaimService;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimDecision;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.DecideClaimRequest;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimResult;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimablePersonDto;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.PersonClaimDto;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.SubmitClaimRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

/**
 * The aspirant claim flow. Claimant-facing endpoints derive the account from
 * the caller and never accept one; the two decision endpoints are the
 * reviewer's and are the only way an account becomes linked to a public
 * figure's record.
 *
 * Each handler is an exhaustive switch over the service's sealed result, as in
 * RegistrationResource, so every outcome has an explicit HTTP mapping.
 */
@Path("person-claims")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMINISTRATOR"})
public class PersonClaimResource {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_SEARCH_LENGTH = 2;

    @Inject
    private PersonClaimService service;

    @Inject
    private CallerAccounts callers;

    /**
     * Candidates for a claim: people already on record whose name matches and
     * who no account holds yet.
     */
    @GET
    @Path("claimable")
    public List<ClaimablePersonDto> claimable(@QueryParam("name") String name,
                                  @QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("20") int size) {
        if (name == null || name.strip().length() < MIN_SEARCH_LENGTH) {
            throw new BadRequestException(
                    "Query parameter 'name' must be at least " + MIN_SEARCH_LENGTH + " characters");
        }
        return service.searchClaimable(name, page, clamp(size));
    }

    @POST
    public Response submit(@Valid SubmitClaimRequest request,
                           @Context SecurityContext security,
                           @Context UriInfo uriInfo) {
        Long accountId = callers.requireId(security);
        return switch (service.submit(accountId, request.personId(), request.evidence())) {
            case ClaimResult.Submitted(PersonClaimDto claim) ->
                    Response.created(uriInfo.getAbsolutePathBuilder()
                                    .path(String.valueOf(claim.id())).build())
                            .entity(claim)
                            .build();
            case ClaimResult.AccountUnavailable() ->
                    error(Status.FORBIDDEN, "Verify your account before claiming a profile");
            case ClaimResult.PersonNotFound(Long personId) ->
                    error(Status.NOT_FOUND, "Person not found: " + personId);
            case ClaimResult.AlreadyLinked(Long personId) ->
                    error(Status.CONFLICT, "This account is already linked to person " + personId);
            case ClaimResult.PersonAlreadyClaimed() ->
                    error(Status.CONFLICT, "Another account already holds this profile");
            case ClaimResult.ClaimAlreadyOpen(PersonClaimDto existing) ->
                    Response.status(Status.CONFLICT)
                            .entity(Map.of("error", "A claim is already awaiting review",
                                    "claim", existing))
                            .build();
        };
    }

    @GET
    @Path("mine")
    public List<PersonClaimDto> mine(@Context SecurityContext security) {
        return service.findByAccount(callers.requireId(security));
    }

    @POST
    @Path("{id}/withdrawal")
    public Response withdraw(@PathParam("id") Long id, @Context SecurityContext security) {
        return decided(service.withdraw(id, callers.requireId(security)));
    }

    @GET
    @Path("pending")
    @RolesAllowed("ADMINISTRATOR")
    public List<PersonClaimDto> pending(@QueryParam("page") @DefaultValue("0") int page,
                                        @QueryParam("size") @DefaultValue("50") int size) {
        return service.findPending(page, clamp(size));
    }

    @POST
    @Path("{id}/approval")
    @RolesAllowed("ADMINISTRATOR")
    public Response approve(@PathParam("id") Long id, @Valid DecideClaimRequest request,
                            @Context SecurityContext security) {
        return decided(service.approve(id, callers.requireId(security), noteOf(request)));
    }

    @POST
    @Path("{id}/rejection")
    @RolesAllowed("ADMINISTRATOR")
    public Response reject(@PathParam("id") Long id, @Valid DecideClaimRequest request,
                           @Context SecurityContext security) {
        return decided(service.reject(id, callers.requireId(security), noteOf(request)));
    }

    private static Response decided(ClaimDecision decision) {
        return switch (decision) {
            case ClaimDecision.Decided(PersonClaimDto claim) -> Response.ok(claim).build();
            case ClaimDecision.ClaimNotFound(Long claimId) ->
                    error(Status.NOT_FOUND, "Claim not found: " + claimId);
            case ClaimDecision.AlreadyDecided(PersonClaimDto claim) ->
                    error(Status.CONFLICT, "Claim was already " + claim.status().toLowerCase());
            // 404, not 403: a claimant who is not the owner should not learn
            // that the claim exists.
            case ClaimDecision.NotYours() -> error(Status.NOT_FOUND, "Claim not found");
            case ClaimDecision.PersonAlreadyClaimed() ->
                    error(Status.CONFLICT, "Another account was linked to this profile "
                            + "while the claim was open");
        };
    }

    private static String noteOf(DecideClaimRequest request) {
        return request == null ? null : request.note();
    }

    // Same error shape as IllegalArgumentExceptionMapper: {"error": "..."}.
    private static Response error(Status status, String message) {
        return Response.status(status).entity(Map.of("error", message)).build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
