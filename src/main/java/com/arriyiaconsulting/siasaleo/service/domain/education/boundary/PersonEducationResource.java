package com.arriyiaconsulting.siasaleo.service.domain.education.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.education.control.EducationService;
import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
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

@Path("education/persons/{personId}/records")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRATOR")
public class PersonEducationResource {
    @Inject private EducationService service;

    @GET
    public List<PersonEducationDto> list(@PathParam("personId") Long personId) {
        return found(() -> service.findByPerson(personId));
    }

    @GET @Path("{id}")
    public PersonEducationDto get(@PathParam("personId") Long personId, @PathParam("id") Long recordId) {
        return found(() -> service.findById(personId, recordId));
    }

    @POST
    public Response add(@PathParam("personId") Long personId, @NotNull @Valid EducationRequest request, @Context UriInfo uriInfo) {
        PersonEducationDto created = found(() -> service.add(personId, request));
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build())
                .entity(created).build();
    }

    @PUT @Path("{id}")
    public PersonEducationDto update(@PathParam("personId") Long personId, @PathParam("id") Long recordId,
                                     @NotNull @Valid EducationRequest request) {
        return found(() -> service.update(personId, recordId, request));
    }

    @DELETE @Path("{id}")
    public void delete(@PathParam("personId") Long personId, @PathParam("id") Long recordId) {
        found(() -> {
            service.delete(personId, recordId);
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
