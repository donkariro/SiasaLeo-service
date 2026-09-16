package com.arriyiaconsulting.siasaleo.service.domain.education.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.education.control.EducationCatalogService;
import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("education")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EducationCatalogResource {
    @Inject private EducationCatalogService service;

    @GET @Path("levels")
    public List<EducationLevelDto> levels() { return service.levels(); }

    @GET @Path("institution-types")
    public List<InstitutionTypeDto> institutionTypes() { return service.institutionTypes(); }

    @GET @Path("fields-of-study")
    public List<FieldOfStudyDto> fields() { return service.fields(); }

    @GET @Path("institutions")
    public List<InstitutionDto> institutions(@QueryParam("typeId") Long typeId,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return service.institutions(typeId, search, page, size);
    }

    @GET @Path("institutions/{id}")
    public InstitutionDto institution(@PathParam("id") Long id) {
        return service.findInstitution(id).orElseThrow(NotFoundException::new);
    }

    @POST @Path("institutions")
    @RolesAllowed("ADMINISTRATOR")
    public Response createInstitution(@NotNull @Valid CreateInstitutionRequest request,
                                      @Context UriInfo uriInfo) {
        InstitutionDto created = service.createInstitution(request);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build())
                .entity(created).build();
    }
}
