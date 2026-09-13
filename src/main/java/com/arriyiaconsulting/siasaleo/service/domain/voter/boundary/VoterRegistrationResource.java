package com.arriyiaconsulting.siasaleo.service.domain.voter.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.RegisterVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * The voter roll itself. Operations on the caller's own registration —
 * transfer, withdrawal, history — live on VoterResource.
 *
 * Registering is the caller's own act, so the person is taken from the
 * authenticated account and never from the payload. The reads are another
 * matter: these registrations are self-declarations by the system's users, not
 * a public register, so listing them is listing who signed up where. Both
 * reads are administrative.
 */
@Path("voter-registrations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMINISTRATOR"})
public class VoterRegistrationResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private VoterRegistrationService service;

    @Inject
    private CallerAccounts callers;

    @GET
    @Path("{id}")
    @RolesAllowed("ADMINISTRATOR")
    public VoterRegistrationDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // Listing without a centre filter would dump every declaration in the
    // system, so centerId is mandatory here.
    @GET
    @RolesAllowed("ADMINISTRATOR")
    public List<VoterRegistrationDto> listByCenter(@QueryParam("centerId") Long centerId,
                                                   @QueryParam("page") @DefaultValue("0") int page,
                                                   @QueryParam("size") @DefaultValue("100") int size) {
        if (centerId == null) {
            throw new BadRequestException("Query parameter 'centerId' is required");
        }
        return service.findByCenter(centerId, page, clamp(size));
    }

    @POST
    public Response register(@Valid RegisterVoterRequest request,
                             @Context SecurityContext security,
                             @Context UriInfo uriInfo) {
        VoterRegistrationDto created = service.register(callers.requireId(security), request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
