package com.arriyiaconsulting.siasaleo.service.domain.education.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.education.control.EducationService;
import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

@Path("education/me/records")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMINISTRATOR"})
public class MyEducationResource {
    @Inject private EducationService service;
    @Inject private CallerAccounts callers;

    @GET
    public List<PersonEducationDto> list(@Context SecurityContext security) {
        return found(() -> service.findFor(callers.requireId(security)));
    }

    @GET @Path("{id}")
    public PersonEducationDto get(@Context SecurityContext security, @PathParam("id") Long recordId) {
        return found(() -> service.findByIdFor(callers.requireId(security), recordId));
    }

    @POST
    public Response add(@Context SecurityContext security, @NotNull @Valid EducationRequest request, @Context UriInfo uriInfo) {
        PersonEducationDto created = found(() -> service.addFor(callers.requireId(security), request));
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build())
                .entity(created).build();
    }

    @PUT @Path("{id}")
    public PersonEducationDto update(@Context SecurityContext security, @PathParam("id") Long recordId,
                                     @NotNull @Valid EducationRequest request) {
        return found(() -> service.updateFor(callers.requireId(security), recordId, request));
    }

    @DELETE @Path("{id}")
    public void delete(@Context SecurityContext security, @PathParam("id") Long recordId) {
        found(() -> {
            service.deleteFor(callers.requireId(security), recordId);
            return null;
        });
    }

    private static <T> T found(Supplier<T> action) {
        try {
            return action.get();
        } catch (NoSuchElementException e) {
            throw new NotFoundException(e.getMessage());
        }
    }
}
