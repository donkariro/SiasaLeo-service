package com.arriyiaconsulting.siasaleo.service.domain.voter.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.RegisterVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
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
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * The voter roll itself. Operations on one voter's registration — transfer,
 * deregistration, history — live on VoterResource.
 */
@Path("voter-registrations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VoterRegistrationResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private VoterRegistrationService service;

    @GET
    @Path("{id}")
    public VoterRegistrationDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // Listing without a centre filter would dump the national roll, so
    // centerId is mandatory here.
    @GET
    public List<VoterRegistrationDto> listByCenter(@QueryParam("centerId") Long centerId,
                                                   @QueryParam("page") @DefaultValue("0") int page,
                                                   @QueryParam("size") @DefaultValue("100") int size) {
        if (centerId == null) {
            throw new BadRequestException("Query parameter 'centerId' is required");
        }
        return service.findByCenter(centerId, page, clamp(size));
    }

    @POST
    public Response register(@Valid RegisterVoterRequest request, @Context UriInfo uriInfo) {
        VoterRegistrationDto created = service.register(request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
