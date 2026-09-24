package com.arriyiaconsulting.siasaleo.service.domain.voter.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegisterService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
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

@Path("voter-registers") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class VoterRegisterResource {
    @Inject private VoterRegisterService service;
    @Context private SecurityContext security;
    private boolean admin() { return security.isUserInRole("ADMINISTRATOR"); }
    @GET public List<RegisterDto> list(@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return service.registers(admin(),page,size); }
    @GET @Path("{id}") public RegisterDto get(@PathParam("id") long id) { return service.find(id,admin()); }
    @POST @Path("validate") @RolesAllowed("ADMINISTRATOR")
    public ValidationReport preview(RegisterRequest r) { return service.preview(r); }
    @POST @RolesAllowed("ADMINISTRATOR")
    @APIResponse(responseCode="201",description="Register draft created",content=@Content(schema=@Schema(implementation=RegisterImportResult.class)))
    @APIResponse(responseCode="200",description="Exact register replay",content=@Content(schema=@Schema(implementation=RegisterImportResult.class)))
    public Response create(@NotNull @Valid RegisterRequest r,@Context UriInfo uri) {
        var result=service.create(r);
        return Response.status(result.replayed()?200:201).location(uri.getAbsolutePathBuilder().path(result.register().id().toString()).build()).entity(result).build();
    }
    @POST @Path("{id}/batches") @RolesAllowed("ADMINISTRATOR")
    public RegisterImportResult append(@PathParam("id") long id,@NotNull @Valid RegisterRequest r) { return service.append(id,r); }
    @GET @Path("{id}/validation") @RolesAllowed("ADMINISTRATOR")
    public ValidationReport validation(@PathParam("id") long id) { return service.validation(id); }
    @POST @Path("{id}/publish") @RolesAllowed("ADMINISTRATOR")
    public RegisterDto publish(@PathParam("id") long id) { return service.publish(id); }
    @DELETE @Path("{id}") @RolesAllowed("ADMINISTRATOR")
    public void delete(@PathParam("id") long id) { service.deleteDraft(id); }
    @GET @Path("{id}/counts") public List<RegisterCount> counts(@PathParam("id") long id,@QueryParam("areaId") Long area,
            @QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return service.counts(id,area,admin(),page,size); }
    @GET @Path("{id}/summary") public RegisterSummary summary(@PathParam("id") long id,@QueryParam("areaId") Long area) { return service.summary(id,area,admin()); }
}
