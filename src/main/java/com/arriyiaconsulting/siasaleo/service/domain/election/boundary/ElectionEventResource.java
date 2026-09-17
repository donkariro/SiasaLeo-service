package com.arriyiaconsulting.siasaleo.service.domain.election.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.election.control.ElectionService;
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

@Path("election-events")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ElectionEventResource {
    @Inject private ElectionService service;

    @GET
    public List<ElectionEventDto> list(@QueryParam("electionCycleId") Long cycleId,
            @QueryParam("typeId") Long typeId, @QueryParam("statusId") Long statusId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return service.events(cycleId, typeId, statusId, page, size);
    }

    @GET @Path("{id}")
    public ElectionEventDto get(@PathParam("id") Long id) {
        return service.findEvent(id).orElseThrow(NotFoundException::new);
    }

    @POST
    @RolesAllowed("ADMINISTRATOR")
    @APIResponse(responseCode = "201", description = "Election event created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ElectionEventDto.class)))
    public Response create(@NotNull @Valid CreateElectionEventRequest request, @Context UriInfo uriInfo) {
        ElectionEventDto created = service.createEvent(request);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build())
                .entity(created).build();
    }

    @PUT @Path("{id}/status")
    @RolesAllowed("ADMINISTRATOR")
    public ElectionEventDto changeStatus(@PathParam("id") Long id,
            @NotNull @Valid ChangeElectionStatusRequest request) {
        return service.changeStatus(id, request).orElseThrow(NotFoundException::new);
    }
}
