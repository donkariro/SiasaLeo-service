package com.arriyiaconsulting.siasaleo.service.domain.candidate.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.control.CandidacyService;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.CandidacyDto;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.RegisterCandidateRequest;
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

@Path("candidacies")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidacyResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private CandidacyService service;

    @GET
    @Path("{id}")
    public CandidacyDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }

    // Listing without a contest filter would dump every candidacy ever
    // recorded, so contestId is mandatory here.
    @GET
    public List<CandidacyDto> listByContest(@QueryParam("contestId") Long contestId,
                                            @QueryParam("page") @DefaultValue("0") int page,
                                            @QueryParam("size") @DefaultValue("100") int size) {
        if (contestId == null) {
            throw new BadRequestException("Query parameter 'contestId' is required");
        }
        return service.findByContest(contestId, page, clamp(size));
    }

    @POST
    public Response register(@Valid RegisterCandidateRequest request, @Context UriInfo uriInfo) {
        CandidacyDto created = service.register(request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
