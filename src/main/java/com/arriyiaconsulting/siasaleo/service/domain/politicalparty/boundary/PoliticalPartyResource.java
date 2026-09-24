package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PoliticalPartyColorService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PoliticalPartyService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PoliticalPartySloganService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PoliticalPartySymbolService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AdoptSloganRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AdoptSymbolRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyColorsDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyOptionDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartySloganDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartySymbolDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.ReplaceColorsRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.RetireSloganRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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
 * The register of political parties, and the symbol, colours and slogans each
 * presents. Those three hang off the party here rather than living on root
 * resources of their own: a root path of political-parties/{id}/... carries
 * more literal characters than this one and would shadow it for every request.
 */
@Path("political-parties")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PoliticalPartyResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private PoliticalPartyService service;

    @Inject
    private PoliticalPartySymbolService symbols;

    @Inject
    private PoliticalPartyColorService colors;

    @Inject
    private PoliticalPartySloganService slogans;

    @GET
    @Path("{id}")
    public PoliticalPartyDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // The register is small and bounded (98 parties as at V10), so unlike
    // candidacies and electoral areas this listing needs no mandatory filter:
    // the default page holds the whole register in one call.
    @GET
    public List<PoliticalPartyDto> list(@QueryParam("page") @DefaultValue("0") int page,
                                        @QueryParam("size") @DefaultValue("100") int size) {
        return service.findAll(page, clamp(size));
    }

    /** All parties for candidate registration and other selection controls. */
    @GET
    @Path("options")
    public List<PoliticalPartyOptionDto> options() {
        return service.findAllOptions();
    }

    /** 404 for a party the register records no symbol for. */
    @GET
    @Path("{id}/symbol")
    public PoliticalPartySymbolDto currentSymbol(@PathParam("id") Long id) {
        return symbols.findCurrent(id).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("{id}/symbols")
    public List<PoliticalPartySymbolDto> symbolHistory(@PathParam("id") Long id) {
        return symbols.findHistory(id);
    }

    // Adopting a symbol replaces the one in use, so this posts to the
    // succession rather than putting to the singular sub-resource.
    @POST
    @RolesAllowed("ADMINISTRATOR")
    @Path("{id}/symbols")
    public Response adoptSymbol(@PathParam("id") Long id,
                                @Valid AdoptSymbolRequest request,
                                @Context UriInfo uriInfo) {
        PoliticalPartySymbolDto created = symbols.adopt(id, request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    @GET
    @Path("{id}/colors")
    public PoliticalPartyColorsDto colors(@PathParam("id") Long id) {
        return colors.findByParty(id);
    }

    // PUT, not POST: colours carry no validity period and are replaced
    // wholesale, so this is idempotent.
    @PUT
    @RolesAllowed("ADMINISTRATOR")
    @Path("{id}/colors")
    public PoliticalPartyColorsDto replaceColors(@PathParam("id") Long id,
                                                 @Valid ReplaceColorsRequest request) {
        return colors.replace(id, request);
    }

    // current=false widens the list from the slogans in use to every one on
    // record; several may be in use at once.
    @GET
    @Path("{id}/slogans")
    public List<PoliticalPartySloganDto> slogans(
            @PathParam("id") Long id,
            @QueryParam("current") @DefaultValue("true") boolean current) {
        return slogans.findByParty(id, current);
    }

    @POST
    @RolesAllowed("ADMINISTRATOR")
    @Path("{id}/slogans")
    public Response adoptSlogan(@PathParam("id") Long id,
                                @Valid AdoptSloganRequest request,
                                @Context UriInfo uriInfo) {
        PoliticalPartySloganDto created = slogans.adopt(id, request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    // The body only carries an optional date, so an empty request retires the
    // slogan today.
    @POST
    @RolesAllowed("ADMINISTRATOR")
    @Path("{id}/slogans/{sloganId}/retirement")
    public PoliticalPartySloganDto retireSlogan(@PathParam("id") Long id,
                                                @PathParam("sloganId") Long sloganId,
                                                @Valid RetireSloganRequest request) {
        return slogans.retire(id, sloganId, request != null ? request.uptoDate() : null);
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
