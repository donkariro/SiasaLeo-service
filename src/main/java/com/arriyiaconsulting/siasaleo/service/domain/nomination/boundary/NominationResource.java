package com.arriyiaconsulting.siasaleo.service.domain.nomination.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormSubmissionDto;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.control.NominationService;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.entity.NominationFormPurpose;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("nominations") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class NominationResource {
    @Inject private NominationService service;
    @Inject private CallerAccounts callers;
    private Long actor(SecurityContext security) {
        return security != null && security.getUserPrincipal() != null ? callers.requireId(security) : null;
    }

    @PUT @Path("parties/{party}/managers/{account}") @RolesAllowed("ADMINISTRATOR")
    public void assignManager(@PathParam("party") long party, @PathParam("account") long account, @Context SecurityContext security) {
        service.manager(party, account, callers.requireId(security), false);
    }
    @DELETE @Path("parties/{party}/managers/{account}") @RolesAllowed("ADMINISTRATOR")
    public void revokeManager(@PathParam("party") long party, @PathParam("account") long account, @Context SecurityContext security) {
        service.manager(party, account, callers.requireId(security), true);
    }
    @GET @Path("parties/{party}/exercises")
    public List<NominationExerciseDto> exercises(@PathParam("party") long party, @Context SecurityContext security) {
        return service.exercises(party, actor(security));
    }

    @POST @Path("exercises") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationExerciseDto create(@NotNull @Valid CreateNominationExerciseRequest request, @Context SecurityContext security) {
        return service.create(request, callers.requireId(security));
    }
    @GET @Path("exercises/{id}")
    public NominationExerciseDto exercise(@PathParam("id") long id, @Context SecurityContext security) {
        return service.exercise(id, actor(security));
    }
    @PUT @Path("exercises/{id}/forms/{purpose}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationExerciseDto bindForm(@PathParam("id") long id, @PathParam("purpose") NominationFormPurpose purpose,
            @NotNull @Valid BindExerciseFormRequest request, @Context SecurityContext security) {
        return service.bindForm(id, purpose, request.formVersionId(), callers.requireId(security));
    }
    @POST @Path("exercises/{id}/open") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationExerciseDto open(@PathParam("id") long id, @NotNull @Valid NominationTransitionRequest request, @Context SecurityContext security) {
        return service.open(id, request.rowVersion(), callers.requireId(security));
    }
    @POST @Path("exercises/{id}/close") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationExerciseDto close(@PathParam("id") long id, @NotNull @Valid NominationTransitionRequest request, @Context SecurityContext security) {
        return service.close(id, request.rowVersion(), callers.requireId(security));
    }
    @GET @Path("exercises/{id}/applications") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public List<NominationApplicationSummaryDto> applications(@PathParam("id") long id, @Context SecurityContext security) {
        return service.applications(id, callers.requireId(security));
    }
    @POST @Path("exercises/{id}/applications") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationApplicationDto apply(@PathParam("id") long id, @NotNull @Valid StartApplicationRequest request, @Context SecurityContext security) {
        return service.apply(id, request.contestId(), callers.requireId(security));
    }

    @GET @Path("applications/mine") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public List<NominationApplicationSummaryDto> mine(@Context SecurityContext security) {
        return service.mine(callers.requireId(security));
    }
    @GET @Path("applications/{id}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationApplicationDto application(@PathParam("id") long id, @Context SecurityContext security) {
        return service.application(id, callers.requireId(security));
    }
    @PUT @Path("applications/{id}/answers") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationApplicationDto save(@PathParam("id") long id, @NotNull @Valid SaveApplicationAnswersRequest request, @Context SecurityContext security) {
        return service.save(id, request, callers.requireId(security));
    }
    @POST @Path("applications/{id}/submit") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public FormSubmissionDto submit(@PathParam("id") long id, @NotNull @Valid SubmitApplicationRequest request, @Context SecurityContext security) {
        return service.submit(id, request, callers.requireId(security));
    }
    @GET @Path("applications/{id}/submissions/{revision}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public FormSubmissionDto submission(@PathParam("id") long id, @PathParam("revision") int revision, @Context SecurityContext security) {
        return service.submission(id, revision, callers.requireId(security));
    }
    @POST @Path("applications/{id}/withdraw") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public NominationApplicationSummaryDto withdraw(@PathParam("id") long id, @NotNull @Valid NominationTransitionRequest request, @Context SecurityContext security) {
        return service.withdraw(id, request.rowVersion(), callers.requireId(security));
    }
}
