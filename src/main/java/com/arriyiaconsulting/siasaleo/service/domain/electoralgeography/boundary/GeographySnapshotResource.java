package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("geography-snapshots")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeographySnapshotResource {
    @Inject private GeographySnapshotService service;
    @Inject private GeographySnapshotPublisher publisher;
    @Context private SecurityContext security;

    @GET
    public List<GeographySnapshotDto> list(@QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return service.list(security.isUserInRole("ADMINISTRATOR"), page, size);
    }

    @GET @Path("{id}")
    public GeographySnapshotDto get(@PathParam("id") Long id) {
        return service.find(id, security.isUserInRole("ADMINISTRATOR"));
    }

    @POST @RolesAllowed("ADMINISTRATOR")
    public Response create(@NotNull @Valid CreateGeographySnapshotRequest request, @Context UriInfo uri) {
        GeographySnapshotDto created = service.create(request);
        return Response.created(uri.getAbsolutePathBuilder().path(created.id().toString()).build()).entity(created).build();
    }

    @PUT @Path("{id}") @RolesAllowed("ADMINISTRATOR")
    public GeographySnapshotDto update(@PathParam("id") Long id, @NotNull @Valid CreateGeographySnapshotRequest request) {
        return service.reviseMetadata(id, request);
    }

    @POST @Path("{id}/publish") @RolesAllowed("ADMINISTRATOR")
    public GeographySnapshotDto publish(@PathParam("id") Long id) { return publisher.publish(id); }

    @POST @Path("{id}/revisions") @RolesAllowed("ADMINISTRATOR")
    public Response revision(@PathParam("id") Long id, @NotNull @Valid CreateGeographySnapshotRequest request,
            @Context UriInfo uri) {
        GeographySnapshotDto created = service.createRevision(id, request);
        return Response.created(uri.getBaseUriBuilder().path("geography-snapshots").path(created.id().toString()).build())
                .entity(created).build();
    }
}
