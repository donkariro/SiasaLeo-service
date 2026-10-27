package com.arriyiaconsulting.siasaleo.service.domain.forms.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormAdministrationService;
import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.*;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

/**
 * Form authoring. In v1 platform administrators write the JSON with each
 * client; a party-facing builder will call these same operations later.
 * Applicants never use this resource: they reach forms through the consuming
 * domain (e.g. /nominations/applications/{id}).
 */
@Path("forms") @RequestScoped @RolesAllowed("ADMINISTRATOR")
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class FormResource {
    @Inject private FormAdministrationService service;
    @Inject private CallerAccounts callers;

    @GET @Path("definitions")
    public List<FormDefinitionDto> definitions(@QueryParam("purpose") String purpose) { return service.definitions(purpose); }
    @POST @Path("definitions")
    public FormDefinitionDto create(@NotNull @Valid CreateFormDefinitionRequest request, @Context SecurityContext security) {
        return service.create(request, callers.requireId(security));
    }
    @GET @Path("definitions/{id}")
    public FormDefinitionDto definition(@PathParam("id") long id) { return service.definition(id); }
    @GET @Path("definitions/{id}/versions")
    public List<FormVersionDto> versions(@PathParam("id") long id) { return service.versions(id); }
    @POST @Path("definitions/{id}/versions")
    public FormVersionDto draft(@PathParam("id") long id, @NotNull @Valid CreateFormVersionRequest request, @Context SecurityContext security) {
        return service.draft(id, request, callers.requireId(security));
    }
    @GET @Path("versions/{id}")
    public FormVersionDto version(@PathParam("id") long id) { return service.version(id); }
    @PUT @Path("versions/{id}")
    public FormVersionDto revise(@PathParam("id") long id, @NotNull @Valid UpdateFormVersionRequest request) {
        return service.revise(id, request);
    }
    @POST @Path("versions/{id}/preview")
    public FormPreviewDto preview(@PathParam("id") long id, @NotNull @Valid PreviewFormRequest request) {
        return service.preview(id, request);
    }
    @POST @Path("versions/{id}/publish")
    public FormVersionDto publish(@PathParam("id") long id, @NotNull @Valid FormVersionTransitionRequest request, @Context SecurityContext security) {
        return service.publish(id, request.rowVersion(), callers.requireId(security));
    }
    @POST @Path("versions/{id}/retire")
    public FormVersionDto retire(@PathParam("id") long id, @NotNull @Valid FormVersionTransitionRequest request) {
        return service.retire(id, request.rowVersion());
    }
}
