package com.arriyiaconsulting.siasaleo.service.electoralgeography.boundary;

import com.arriyiaconsulting.siasaleo.service.electoralgeography.control.ElectoralAreaService;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.dto.CreateElectoralAreaRequest;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.dto.ElectoralAreaDto;
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

@Path("electoral-areas")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ElectoralAreaResource {

    private static final int MAX_PAGE_SIZE = 500;

    private ElectoralAreaService service;

    ElectoralAreaResource() {
    }

    @Inject
    public ElectoralAreaResource(ElectoralAreaService service) {
        this.service = service;
    }

    @GET
    @Path("{id}")
    public ElectoralAreaDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // Listing without a type filter would dump the whole register, so type is
    // mandatory here; use /{id}/children or /{id}/subtree for tree navigation.
    @GET
    public List<ElectoralAreaDto> listByType(@QueryParam("type") String type,
                                             @QueryParam("page") @DefaultValue("0") int page,
                                             @QueryParam("size") @DefaultValue("100") int size) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Query parameter 'type' is required");
        }
        return service.findByType(type, page, clamp(size));
    }

    @GET
    @Path("{id}/children")
    public List<ElectoralAreaDto> children(@PathParam("id") Long id) {
        return service.findChildren(id).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("{id}/subtree")
    public List<ElectoralAreaDto> subtree(@PathParam("id") Long id,
                                          @QueryParam("page") @DefaultValue("0") int page,
                                          @QueryParam("size") @DefaultValue("100") int size) {
        return service.findDescendants(id, page, clamp(size))
                .orElseThrow(NotFoundException::new);
    }

    @POST
    public Response create(@Valid CreateElectoralAreaRequest request, @Context UriInfo uriInfo) {
        ElectoralAreaDto created = service.create(request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
