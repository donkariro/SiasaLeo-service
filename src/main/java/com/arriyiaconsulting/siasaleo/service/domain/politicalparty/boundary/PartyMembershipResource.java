package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PartyMembershipService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.JoinPartyRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PartyMembershipDto;
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
 * Party membership rolls. Operations on one member's standing — defection,
 * resignation, history — live on PartyMemberResource.
 */
@Path("party-memberships")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartyMembershipResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private PartyMembershipService service;

    @GET
    @Path("{id}")
    public PartyMembershipDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // Listing without a party filter would dump every membership on record, so
    // politicalPartyId is mandatory here.
    @GET
    public List<PartyMembershipDto> listByParty(
            @QueryParam("politicalPartyId") Long politicalPartyId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        if (politicalPartyId == null) {
            throw new BadRequestException("Query parameter 'politicalPartyId' is required");
        }
        return service.findByParty(politicalPartyId, page, clamp(size));
    }

    @POST
    public Response join(@Valid JoinPartyRequest request, @Context UriInfo uriInfo) {
        PartyMembershipDto created = service.join(request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
