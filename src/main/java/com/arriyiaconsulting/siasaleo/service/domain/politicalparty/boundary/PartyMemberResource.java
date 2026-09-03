package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PartyMembershipService;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.DefectToPartyRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PartyMembershipDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.ResignMembershipRequest;
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
 * One person's standing as a party member, addressed by their person id. A
 * member belongs to at most one party, so /membership is a singular
 * sub-resource; /memberships is the trail behind it. Kept under this module's
 * own path rather than persons/{personId} so it does not shadow a future
 * person resource.
 */
@Path("party-members/{personId}")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartyMemberResource {

    @Inject
    private PartyMembershipService service;

    /** 404 once the member resigns: they then belong to no party. */
    @GET
    @Path("membership")
    public PartyMembershipDto current(@PathParam("personId") Long personId) {
        return service.findCurrentByPerson(personId).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("memberships")
    public List<PartyMembershipDto> history(@PathParam("personId") Long personId) {
        return service.findHistory(personId);
    }

    @POST
    @Path("defection")
    public PartyMembershipDto defect(@PathParam("personId") Long personId,
                                     @Valid DefectToPartyRequest request) {
        return service.defect(personId, request);
    }

    // The body only carries an optional date, so an empty request means
    // "resigned today".
    @POST
    @Path("resignation")
    public PartyMembershipDto resign(@PathParam("personId") Long personId,
                                     @Valid ResignMembershipRequest request) {
        return service.resign(personId, request != null ? request.endDate() : null);
    }
}
