package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.control.ManifestoService;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto.*;
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

@Path("governance/manifestos") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ManifestoResource {
    @Inject private ManifestoService service;
    @Inject private CallerAccounts callers;
    @POST @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoDto create(@NotNull @Valid CreateManifestoRequest request, @Context SecurityContext security) { return service.create(request, callers.requireId(security)); }

    @POST @Path("{id}/versions") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoVersionDto createVersion(@PathParam("id") long id, @NotNull @Valid CreateManifestoVersionRequest request, @Context SecurityContext security) {
        return service.createVersion(id, request, callers.requireId(security));
    }

    @POST @Path("{id}/goals") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public GoalDto createGoal(@PathParam("id") long id, @NotNull @Valid CreateGoalRequest request, @Context SecurityContext security) {
        return service.createGoal(id, request, callers.requireId(security));
    }

    @POST @Path("{id}/commitments") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentRevisionDto createCommitment(@PathParam("id") long id, @NotNull @Valid CreateCommitmentRevisionRequest request, @Context SecurityContext security) {
        return service.createCommitment(id, request, callers.requireId(security));
    }

    @POST @Path("commitments/{id}/revisions") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentRevisionDto createRevision(@PathParam("id") long id, @NotNull @Valid CreateCommitmentRevisionRequest request, @Context SecurityContext security) {
        return service.createRevision(id, request, callers.requireId(security));
    }

    @POST @Path("versions/{id}/commitments") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoCommitmentDto attachCommitment(@PathParam("id") long id, @NotNull @Valid CommitmentLinkRequest request, @Context SecurityContext security) {
        return service.attachCommitment(id, request, callers.requireId(security));
    }

    @POST @Path("revisions/{id}/targets") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentTargetDto createTarget(@PathParam("id") long id, @NotNull @Valid CreateCommitmentTargetRequest request, @Context SecurityContext security) {
        return service.createTarget(id, request, callers.requireId(security));
    }

    @POST @Path("revisions/{id}/goals") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentGoalDto attachGoal(@PathParam("id") long id, @NotNull @Valid GoalLinkRequest request, @Context SecurityContext security) {
        return service.attachGoal(id, request, callers.requireId(security));
    }

    @POST @Path("revisions/{id}/policy-areas") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentPolicyAreaDto classify(@PathParam("id") long id, @NotNull @Valid PolicyAreaRequest request, @Context SecurityContext security) {
        return service.classify(id, request, callers.requireId(security));
    }

    @POST @Path("versions/{id}/assets") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoAttachmentDto attachVersionAsset(@PathParam("id") long id, @NotNull @Valid AttachmentRequest request, @Context SecurityContext security) {
        return service.attachVersionAsset(id, request, callers.requireId(security));
    }

    @POST @Path("revisions/{id}/assets") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentAttachmentDto attachRevisionAsset(@PathParam("id") long id, @NotNull @Valid AttachmentRequest request, @Context SecurityContext security) {
        return service.attachRevisionAsset(id, request, callers.requireId(security));
    }

    @POST @Path("versions/{id}/announcements") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public PublicationEventDto announce(@PathParam("id") long id, @NotNull @Valid CreatePublicationEventRequest request, @Context SecurityContext security) {
        return service.announce(id, request, callers.requireId(security));
    }

    @PATCH @Path("versions/{id}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoVersionDto editVersion(@PathParam("id") long id, @NotNull @Valid PatchManifestoVersionRequest request, @Context SecurityContext security) { return service.editVersion(id, request, callers.requireId(security)); }
    @PATCH @Path("revisions/{id}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public CommitmentRevisionDto editRevision(@PathParam("id") long id, @NotNull @Valid PatchCommitmentRevisionRequest request, @Context SecurityContext security) { return service.editRevision(id, request, callers.requireId(security)); }
    @POST @Path("versions/{id}/publish") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoVersionDto publish(@PathParam("id") long id, @Context SecurityContext security) { return service.publish(id, callers.requireId(security), false); }
    @POST @Path("versions/{id}/withdraw") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoVersionDto withdraw(@PathParam("id") long id, @Context SecurityContext security) { return service.publish(id, callers.requireId(security), true); }
    @GET @Path("versions/{id}")
    public ManifestoDetailDto get(@PathParam("id") long id) { return service.version(id, null); }
    @GET @Path("me/versions/{id}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public ManifestoDetailDto draft(@PathParam("id") long id, @Context SecurityContext security) { return service.version(id, callers.requireId(security)); }
    @GET @Path("people/{id}")
    public List<ManifestoVersionDto> list(@PathParam("id") long id, @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size) { return service.list(id, page, size); }
    @GET @Path("revisions/{id}/delivery")
    public CommitmentDeliveryDto delivery(@PathParam("id") long id) { return service.timeline(id); }
}
