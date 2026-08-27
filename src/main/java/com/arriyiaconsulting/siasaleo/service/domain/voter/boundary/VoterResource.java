package com.arriyiaconsulting.siasaleo.service.domain.voter.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.TransferVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
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
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * One voter's place on the roll, addressed by their person id. A voter has at
 * most one current registration, so /registration is a singular sub-resource;
 * /registrations is the trail behind it.
 */
@Path("voters/{personId}")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VoterResource {

    @Inject
    private VoterRegistrationService service;

    /** 404 once the voter is deregistered: they hold no current registration. */
    @GET
    @Path("registration")
    public VoterRegistrationDto current(@PathParam("personId") Long personId) {
        return service.findCurrent(personId).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("registrations")
    public List<VoterRegistrationDto> history(@PathParam("personId") Long personId) {
        return service.findHistory(personId);
    }

    @POST
    @Path("transfer")
    public VoterRegistrationDto transfer(@PathParam("personId") Long personId,
                                         @Valid TransferVoterRequest request) {
        return service.transfer(personId, request);
    }

    @POST
    @Path("deregistration")
    public VoterRegistrationDto deregister(@PathParam("personId") Long personId) {
        return service.deregister(personId);
    }
}
