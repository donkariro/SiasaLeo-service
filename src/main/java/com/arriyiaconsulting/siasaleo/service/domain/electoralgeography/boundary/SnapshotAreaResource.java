package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control.SnapshotAreaService;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("geography-snapshots/{snapshotId}/areas")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapshotAreaResource {
    @Inject private SnapshotAreaService service;
    @PathParam("snapshotId") private Long snapshotId;
    @Context private SecurityContext security;

    @GET
    public List<ElectoralAreaSnapshotDto> list(@QueryParam("type") String type,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size) {
        return service.byType(snapshotId, type, security.isUserInRole("ADMINISTRATOR"), page, size);
    }

    @GET @Path("{id}")
    public ElectoralAreaSnapshotDto get(@PathParam("id") Long id) {
        return service.find(snapshotId, id, security.isUserInRole("ADMINISTRATOR"));
    }

    @GET @Path("{id}/children")
    public List<ElectoralAreaSnapshotDto> children(@PathParam("id") Long id,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size) {
        return service.children(snapshotId, id, security.isUserInRole("ADMINISTRATOR"), page, size);
    }

    @GET @Path("{id}/subtree")
    public List<ElectoralAreaSnapshotDto> subtree(@PathParam("id") Long id,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size) {
        return service.descendants(snapshotId, id, security.isUserInRole("ADMINISTRATOR"), page, size);
    }

    @POST @RolesAllowed("ADMINISTRATOR")
    public Response create(@NotNull @Valid CreateSnapshotAreaRequest request, @Context UriInfo uri) {
        ElectoralAreaSnapshotDto created = service.create(snapshotId, request);
        return Response.created(uri.getAbsolutePathBuilder().path(created.id().toString()).build()).entity(created).build();
    }

    @PUT @Path("{id}") @RolesAllowed("ADMINISTRATOR")
    public ElectoralAreaSnapshotDto update(@PathParam("id") Long id, @NotNull @Valid UpdateSnapshotAreaRequest request) {
        return service.update(snapshotId, id, request);
    }

    @DELETE @Path("{id}") @RolesAllowed("ADMINISTRATOR")
    public void delete(@PathParam("id") Long id) { service.delete(snapshotId, id); }

    @GET @Path("{id}/correspondence") @RolesAllowed("ADMINISTRATOR")
    public AreaCorrespondenceDto correspondence(@PathParam("id") Long id) {
        return service.correspondence(snapshotId, id);
    }

    @PUT @Path("{id}/correspondence") @RolesAllowed("ADMINISTRATOR")
    public AreaCorrespondenceDto review(@PathParam("id") Long id, @NotNull @Valid ReviewAreaCorrespondenceRequest request) {
        return service.reviewCorrespondence(snapshotId, id, request);
    }
}
