package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PoliticalPartyOfficialService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AppointOfficialRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyOfficialDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.StepDownOfficialRequest;
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
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Tenures in party offices. A person may hold several at once, so a tenure is
 * addressed by its own id rather than by the person holding it — which is why
 * standing down hangs off {id} and not off a person path, unlike the singular
 * membership on PartyMemberResource.
 */
@Path("party-officials")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PoliticalPartyOfficialResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private PoliticalPartyOfficialService service;

    @GET
    @Path("{id}")
    public PoliticalPartyOfficialDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // Listing without a party filter would dump every tenure on record, so
    // politicalPartyId is mandatory here. current=false widens the list from
    // the officials in office to the whole succession.
    @GET
    public List<PoliticalPartyOfficialDto> listByParty(
            @QueryParam("politicalPartyId") Long politicalPartyId,
            @QueryParam("current") @DefaultValue("true") boolean current,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        if (politicalPartyId == null) {
            throw new BadRequestException("Query parameter 'politicalPartyId' is required");
        }
        return service.findByParty(politicalPartyId, current, page, clamp(size));
    }

    @GET
    @Path("persons/{personId}")
    public List<PoliticalPartyOfficialDto> listByPerson(@PathParam("personId") Long personId) {
        return service.findByPerson(personId);
    }

    @POST
    @RolesAllowed("ADMINISTRATOR")
    public Response appoint(@Valid AppointOfficialRequest request, @Context UriInfo uriInfo) {
        PoliticalPartyOfficialDto created = service.appoint(request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    // The body only carries an optional date, so an empty request means the
    // official stood down today.
    @POST
    @RolesAllowed("ADMINISTRATOR")
    @Path("{id}/step-down")
    public PoliticalPartyOfficialDto stepDown(@PathParam("id") Long id,
                                              @Valid StepDownOfficialRequest request) {
        return service.stepDown(id, request != null ? request.uptoDate() : null);
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
