package com.arriyiaconsulting.siasaleo.service.domain.election.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.election.control.ContestService;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("contests")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContestResource {
    @Inject private ContestService service;

    @GET
    public List<ContestDto> list(@QueryParam("electionEventId") @NotNull Long eventId,
            @QueryParam("seatId") Long seatId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return service.findByEvent(eventId, seatId, page, size);
    }

    @GET @Path("{id}")
    public ContestDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    @POST
    @RolesAllowed("ADMINISTRATOR")
    @APIResponse(responseCode = "201", description = "Contest created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ContestDto.class)))
    public Response create(@NotNull @Valid CreateContestRequest request, @Context UriInfo uriInfo) {
        ContestDto created = service.create(request);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build())
                .entity(created).build();
    }

    @PUT @Path("{id}/jurisdiction") @RolesAllowed("ADMINISTRATOR")
    public ContestDto assignJurisdiction(@PathParam("id") Long id, @NotNull @Valid AssignJurisdictionRequest request) {
        return service.assignJurisdiction(id,request.officeId(),request.jurisdictionId()).orElseThrow(NotFoundException::new);
    }
}
