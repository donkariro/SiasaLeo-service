package com.arriyiaconsulting.siasaleo.service.domain.voter.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.TransferVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

/**
 * A voter's place on the roll. The caller's own is addressed as "me" — a
 * voter's registration is theirs to read and move, and routing it through an
 * id would mean trusting the requester to name themselves honestly. Reading
 * somebody else's is administrative, and the person id is the address there.
 *
 * /registration is a singular sub-resource, since a voter holds at most one
 * current registration; /registrations is the trail behind it.
 */
@Path("voters")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMINISTRATOR"})
public class VoterResource {

    @Inject
    private VoterRegistrationService service;

    @Inject
    private CallerAccounts callers;

    /** 404 until the caller declares a registration, and once they withdraw it. */
    @GET
    @Path("me/registration")
    public VoterRegistrationDto current(@Context SecurityContext security) {
        return service.findCurrentFor(callers.requireId(security))
                .orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("me/registrations")
    public List<VoterRegistrationDto> history(@Context SecurityContext security) {
        return service.findHistoryFor(callers.requireId(security));
    }

    @POST
    @Path("me/transfer")
    public VoterRegistrationDto transfer(@Valid TransferVoterRequest request,
                                         @Context SecurityContext security) {
        return service.transfer(callers.requireId(security), request);
    }

    @POST
    @Path("me/deregistration")
    public VoterRegistrationDto deregister(@Context SecurityContext security) {
        return service.deregister(callers.requireId(security));
    }

    // JAX-RS matches literal segments ahead of templates, so "me" above is
    // never captured as a person id here.
    @GET
    @Path("{personId}/registration")
    @RolesAllowed("ADMINISTRATOR")
    public VoterRegistrationDto currentOf(@PathParam("personId") Long personId) {
        return service.findCurrent(personId).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("{personId}/registrations")
    @RolesAllowed("ADMINISTRATOR")
    public List<VoterRegistrationDto> historyOf(@PathParam("personId") Long personId) {
        return service.findHistory(personId);
    }
}
