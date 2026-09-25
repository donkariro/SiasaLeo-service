package com.arriyiaconsulting.siasaleo.service.domain.election.result.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.election.result.control.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.result.dto.ResultDtos.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.ValidationReport;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.*;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("result-publications") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ResultPublicationResource {
    @Inject private ResultPublicationService service;
    @Inject private ResultReadService reads;
    @Context private SecurityContext security;
    private boolean admin() { return security.isUserInRole("ADMINISTRATOR"); }
    @GET public List<PublicationDto> list(@QueryParam("contestId") Long contest,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return service.publications(contest,admin(),page,size); }
    @GET @Path("{id}") public PublicationDto get(@PathParam("id") long id) { return service.find(id,admin()); }
    @POST @Path("validate") @RolesAllowed("ADMINISTRATOR") public ValidationReport preview(ResultRequest r) { return service.preview(r); }
    @POST @RolesAllowed("ADMINISTRATOR")
    @APIResponse(responseCode="201",description="Result draft created",content=@Content(schema=@Schema(implementation=ResultImportResult.class)))
    @APIResponse(responseCode="200",description="Exact result replay",content=@Content(schema=@Schema(implementation=ResultImportResult.class)))
    public Response create(@NotNull @Valid ResultRequest r,@Context UriInfo uri) {
        var result=service.create(r);
        return Response.status(result.replayed()?200:201).location(uri.getAbsolutePathBuilder().path(result.publication().id().toString()).build()).entity(result).build();
    }
    @POST @Path("{id}/batches/validate") @RolesAllowed("ADMINISTRATOR")
    public ValidationReport previewBatch(@PathParam("id") long id,ResultRequest r) { return service.previewBatch(id,r); }
    @POST @Path("{id}/batches") @RolesAllowed("ADMINISTRATOR")
    public ResultImportResult append(@PathParam("id") long id,@NotNull @Valid ResultRequest r) { return service.append(id,r); }
    @GET @Path("{id}/validation") @RolesAllowed("ADMINISTRATOR") public ValidationReport validation(@PathParam("id") long id) { return service.validation(id); }
    @POST @Path("{id}/publish") @RolesAllowed("ADMINISTRATOR") public PublicationDto publish(@PathParam("id") long id) { return service.publish(id); }
    @DELETE @Path("{id}") @RolesAllowed("ADMINISTRATOR") public void delete(@PathParam("id") long id) { service.deleteDraft(id); }
    @GET @Path("{id}/candidates") public List<CandidateRow> candidates(@PathParam("id") long id,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return reads.candidates(id,admin(),page,size); }
    @GET @Path("{id}/votes") public List<VoteRow> votes(@PathParam("id") long id,@QueryParam("areaId") Long area,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return reads.votes(id,area,admin(),page,size); }
    @GET @Path("{id}/ballots") public List<BallotRow> ballots(@PathParam("id") long id,@QueryParam("areaId") Long area,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return reads.ballots(id,area,admin(),page,size); }
    @GET @Path("{id}/summary") public ResultSummary summary(@PathParam("id") long id,@QueryParam("areaId") Long area) { return reads.results(id,area,admin()); }
}
