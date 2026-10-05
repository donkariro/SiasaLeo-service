package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("governance/performance") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class PerformanceResource {
    @Inject private WorkEffortService work;
    @Inject private ClaimService claims;
    @Inject private ObservationService observations;
    @Inject private AssessmentService assessments;
    @Inject private CallerAccounts callers;

    @POST @Path("work-efforts") @RolesAllowed("ADMINISTRATOR")
    public WorkEffortDto createWork(@NotNull @Valid CreateWorkEffortRequest request, @Context SecurityContext security) {
        return work.createWork(request, callers.requireId(security));
    }

    @POST @Path("participations") @RolesAllowed("ADMINISTRATOR")
    public WorkEffortParticipationDto createParticipation(@NotNull @Valid CreateWorkEffortParticipationRequest request, @Context SecurityContext security) {
        return work.createParticipation(request, callers.requireId(security));
    }

    @POST @Path("claims") @RolesAllowed("ADMINISTRATOR")
    public ClaimRevisionDto createClaim(@NotNull @Valid CreateClaimRevisionRequest request, @Context SecurityContext security) {
        return claims.createClaim(request, callers.requireId(security));
    }

    @POST @Path("observations") @RolesAllowed("ADMINISTRATOR")
    public ObservationDto createObservation(@NotNull @Valid CreateObservationRequest request, @Context SecurityContext security) {
        return observations.createObservation(request, callers.requireId(security));
    }

    @POST @Path("targets") @RolesAllowed("ADMINISTRATOR")
    public PerformanceTargetDto createTarget(@NotNull @Valid CreatePerformanceTargetRequest request) {
        return observations.createTarget(request);
    }

    @POST @Path("target-observations") @RolesAllowed("ADMINISTRATOR")
    public TargetObservationDto compare(@NotNull @Valid CreateTargetObservationRequest request) {
        return observations.compare(request);
    }

    @POST @Path("delivery-links") @RolesAllowed("ADMINISTRATOR")
    public CommitmentDeliveryLinkDto createDeliveryLink(@NotNull @Valid CreateCommitmentDeliveryLinkRequest request, @Context SecurityContext security) {
        return assessments.createDeliveryLink(request, callers.requireId(security));
    }

    @POST @Path("assessments") @RolesAllowed("ADMINISTRATOR")
    public AssessmentDto createAssessment(@NotNull @Valid CreateAssessmentRequest request, @Context SecurityContext security) {
        return assessments.createAssessment(request, callers.requireId(security));
    }

    @PATCH @Path("claims/{id}") @RolesAllowed("ADMINISTRATOR")
    public ClaimRevisionDto editClaim(@PathParam("id") long id, @NotNull @Valid PatchClaimRevisionRequest request) { return claims.editClaim(id, request); }
    @GET @Path("claims/{id}")
    public ClaimRevisionDto readClaim(@PathParam("id") long id) { return claims.readClaim(id, false); }
    @GET @Path("editor/claims/{id}") @RolesAllowed("ADMINISTRATOR")
    public ClaimRevisionDto draftClaim(@PathParam("id") long id) { return claims.readClaim(id, true); }
    @POST @Path("claims/{id}/publish") @RolesAllowed("ADMINISTRATOR")
    public ClaimRevisionDto publishClaim(@PathParam("id") long id) { return claims.publishClaim(id, false); }
    @POST @Path("claims/{id}/withdraw") @RolesAllowed("ADMINISTRATOR")
    public ClaimRevisionDto withdrawClaim(@PathParam("id") long id) { return claims.publishClaim(id, true); }
    @POST @Path("claims/{id}/evidence") @RolesAllowed("ADMINISTRATOR")
    public ClaimEvidenceDto evidenceClaim(@PathParam("id") long id, @NotNull @Valid EvidenceRequest request) { return claims.addClaimEvidence(id, request); }

    @PATCH @Path("observations/{id}") @RolesAllowed("ADMINISTRATOR")
    public ObservationDto editObservation(@PathParam("id") long id, @NotNull @Valid PatchObservationRequest request) { return observations.editObservation(id, request); }
    @GET @Path("observations/{id}")
    public ObservationDto readObservation(@PathParam("id") long id) { return observations.readObservation(id, false); }
    @GET @Path("editor/observations/{id}") @RolesAllowed("ADMINISTRATOR")
    public ObservationDto draftObservation(@PathParam("id") long id) { return observations.readObservation(id, true); }
    @POST @Path("observations/{id}/publish") @RolesAllowed("ADMINISTRATOR")
    public ObservationDto publishObservation(@PathParam("id") long id) { return observations.publishObservation(id, false); }
    @POST @Path("observations/{id}/withdraw") @RolesAllowed("ADMINISTRATOR")
    public ObservationDto withdrawObservation(@PathParam("id") long id) { return observations.publishObservation(id, true); }
    @POST @Path("observations/{id}/evidence") @RolesAllowed("ADMINISTRATOR")
    public ObservationEvidenceDto evidenceObservation(@PathParam("id") long id, @NotNull @Valid EvidenceRequest request) { return observations.addObservationEvidence(id, request); }

    @PATCH @Path("assessments/{id}") @RolesAllowed("ADMINISTRATOR")
    public AssessmentDto editAssessment(@PathParam("id") long id, @NotNull @Valid PatchAssessmentRequest request) { return assessments.editAssessment(id, request); }
    @GET @Path("assessments/{id}")
    public AssessmentDto readAssessment(@PathParam("id") long id) { return assessments.readAssessment(id, false); }
    @GET @Path("editor/assessments/{id}") @RolesAllowed("ADMINISTRATOR")
    public AssessmentDto draftAssessment(@PathParam("id") long id) { return assessments.readAssessment(id, true); }
    @POST @Path("assessments/{id}/publish") @RolesAllowed("ADMINISTRATOR")
    public AssessmentDto publishAssessment(@PathParam("id") long id) { return assessments.publishAssessment(id, false); }
    @POST @Path("assessments/{id}/withdraw") @RolesAllowed("ADMINISTRATOR")
    public AssessmentDto withdrawAssessment(@PathParam("id") long id) { return assessments.publishAssessment(id, true); }
    @POST @Path("assessments/{id}/evidence") @RolesAllowed("ADMINISTRATOR")
    public AssessmentEvidenceDto evidenceAssessment(@PathParam("id") long id, @NotNull @Valid EvidenceRequest request) { return assessments.addAssessmentEvidence(id, request); }

    @POST @Path("work-efforts/{id}/evidence") @RolesAllowed("ADMINISTRATOR")
    public WorkEffortAssetDto workEvidence(@PathParam("id") long id, @NotNull @Valid AttachmentRequest request) { return work.addWorkAsset(id, request); }
    @POST @Path("claims/{id}/policy-areas/{policyId}") @RolesAllowed("ADMINISTRATOR")
    public ClaimPolicyAreaDto classifyClaim(@PathParam("id") long id, @PathParam("policyId") long policy) { return claims.classifyClaim(id, policy); }
    @POST @Path("work-efforts/{id}/policy-areas/{policyId}") @RolesAllowed("ADMINISTRATOR")
    public WorkEffortPolicyAreaDto classifyWork(@PathParam("id") long id, @PathParam("policyId") long policy) { return work.classifyWork(id, policy); }
    @GET @Path("people/{id}/track-record")
    public List<TrackRecordEntryDto> trackRecord(@PathParam("id") long id, @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size) { return claims.trackRecord(id, page, size); }
    @GET @Path("people/{id}/contributions")
    public List<ContributionDto> contributions(@PathParam("id") long id, @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size) { return work.contributions(id, page, size); }
}
