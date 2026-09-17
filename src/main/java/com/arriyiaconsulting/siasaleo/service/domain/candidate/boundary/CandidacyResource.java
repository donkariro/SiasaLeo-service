package com.arriyiaconsulting.siasaleo.service.domain.candidate.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.control.CandidacyService;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.CandidacyDto;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.RegisterCandidateRequest;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.CandidateRegistrationFormDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("candidacies")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidacyResource {

    private static final int MAX_PAGE_SIZE = 500;

    @Inject
    private CandidacyService service;

    @Inject
    private CallerAccounts callers;

    @GET
    @Path("me/registration-form")
    @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CandidateRegistrationFormDto registrationForm(@Context SecurityContext security) {
        return service.registrationForm(callers.requireId(security));
    }

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
    @RolesAllowed({"USER", "ADMINISTRATOR"})
    @APIResponse(responseCode = "201", description = "Candidate registered",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CandidacyDto.class)))
    public Response register(@NotNull @Valid RegisterCandidateRequest request, @Context UriInfo uriInfo,
                             @Context SecurityContext security) {
        CandidacyDto created = service.register(callers.requireId(security), request);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                        .path(String.valueOf(created.id())).build())
                .entity(created)
                .build();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
