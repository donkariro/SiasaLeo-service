package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("digital-assets") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class DigitalAssetResource {
    @Inject private DigitalAssetService service;
    @Inject private CallerAccounts callers;
    private Long actor(SecurityContext security) {
        return security != null && security.getUserPrincipal() != null ? callers.requireId(security) : null;
    }
    @GET
    public List<DigitalAssetDto> list(@Context SecurityContext security,
            @QueryParam("q") String term, @QueryParam("type") String type,
            @QueryParam("include_archived") @DefaultValue("false") boolean archived,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size) {
        return service.list(actor(security), term, type, archived, page, size);
    }
    @GET @Path("{id}")
    public DigitalAssetDto asset(@PathParam("id") long id, @Context SecurityContext security) {
        return service.asset(id, actor(security));
    }
    @GET @Path("{id}/versions")
    public List<DigitalAssetVersionDto> history(@PathParam("id") long id, @Context SecurityContext security,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size) {
        return service.history(id, actor(security), page, size);
    }
    @PUT @Path("{id}") @RolesAllowed({"USER","ADMINISTRATOR"})
    public DigitalAssetDto update(@PathParam("id") long id, @NotNull @Valid UpdateAssetRequest request, @Context SecurityContext security) {
        return service.update(id, request, callers.requireId(security));
    }
    @PUT @Path("{id}/visibility") @RolesAllowed({"USER","ADMINISTRATOR"})
    public DigitalAssetDto visibility(@PathParam("id") long id, @NotNull @Valid AssetAccessRequest request, @Context SecurityContext security) {
        return service.visibility(id, request.visibility(), callers.requireId(security));
    }
    @POST @Path("{id}/archive") @RolesAllowed({"USER","ADMINISTRATOR"})
    public DigitalAssetDto archive(@PathParam("id") long id, @Context SecurityContext security) {
        return service.archive(id, callers.requireId(security), true);
    }
    @POST @Path("{id}/restore") @RolesAllowed({"USER","ADMINISTRATOR"})
    public DigitalAssetDto restore(@PathParam("id") long id, @Context SecurityContext security) {
        return service.archive(id, callers.requireId(security), false);
    }
    @GET @Path("{id}/grants") @RolesAllowed({"USER","ADMINISTRATOR"})
    public List<AssetGrantDto> grants(@PathParam("id") long id, @Context SecurityContext security) {
        return service.grants(id, callers.requireId(security));
    }
    @PUT @Path("{id}/grants/{account}") @RolesAllowed({"USER","ADMINISTRATOR"})
    public void grant(@PathParam("id") long id, @PathParam("account") long account,
            @NotNull @Valid AssetGrantRequest request, @Context SecurityContext security) {
        service.grant(id, account, request.permission(), callers.requireId(security));
    }
    @DELETE @Path("{id}/grants/{account}") @RolesAllowed({"USER","ADMINISTRATOR"})
    public void revoke(@PathParam("id") long id, @PathParam("account") long account, @Context SecurityContext security) {
        service.revoke(id, account, callers.requireId(security));
    }
    @PUT @Path("{id}/owner") @RolesAllowed({"USER","ADMINISTRATOR"})
    public DigitalAssetDto transfer(@PathParam("id") long id, @NotNull @Valid AssetOwnerRequest request, @Context SecurityContext security) {
        return service.transfer(id, request, callers.requireId(security), security.isUserInRole("ADMINISTRATOR"));
    }
    @GET @Path("{id}/audit") @RolesAllowed({"USER","ADMINISTRATOR"})
    public List<AssetAuditDto> audit(@PathParam("id") long id, @Context SecurityContext security,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size) {
        return service.audit(id, callers.requireId(security), page, size);
    }
    @DELETE @Path("versions/{id}") @RolesAllowed({"USER","ADMINISTRATOR"})
    public void deleteDraft(@PathParam("id") long id, @Context SecurityContext security) {
        service.deleteDraft(id, callers.requireId(security));
    }
    @DELETE @Path("{id}") @RolesAllowed({"USER","ADMINISTRATOR"})
    public void deleteEmpty(@PathParam("id") long id, @Context SecurityContext security) {
        service.deleteEmpty(id, callers.requireId(security));
    }
    @GET @Path("types")
    public List<DigitalAssetTypeDto> types() { return service.types(); }
    @POST @RolesAllowed({"USER", "ADMINISTRATOR"})
    public DigitalAssetDto create(@NotNull @Valid CreateDigitalAssetRequest request, @Context SecurityContext security) {
        return service.create(request, callers.requireId(security));
    }
    @POST @Path("{id}/versions") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public DigitalAssetVersionDto version(@PathParam("id") long id, @NotNull @Valid CreateDigitalAssetVersionRequest request, @Context SecurityContext security) {
        return service.version(id, request, callers.requireId(security));
    }
    @POST @Path("versions/{id}/files") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public StoredFileDto file(@PathParam("id") long id, @NotNull @Valid CreateStoredFileRequest request, @Context SecurityContext security) {
        return service.file(id, request, callers.requireId(security));
    }
    @POST @Path("versions/{id}/seal") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public DigitalAssetDetailDto seal(@PathParam("id") long id, @Context SecurityContext security) { return service.seal(id, callers.requireId(security)); }
    @GET @Path("versions/{id}")
    public DigitalAssetDetailDto read(@PathParam("id") long id, @Context SecurityContext security) { return service.read(id, actor(security)); }
    @GET @Path("me/versions/{id}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public DigitalAssetDetailDto draft(@PathParam("id") long id, @Context SecurityContext security) { return service.read(id, callers.requireId(security)); }
    @GET @Path("{id}/current")
    public DigitalAssetDetailDto current(@PathParam("id") long id, @Context SecurityContext security) { return service.current(id, actor(security)); }
    @GET @Path("me/{id}/current") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public DigitalAssetDetailDto ownCurrent(@PathParam("id") long id, @Context SecurityContext security) { return service.current(id, callers.requireId(security)); }
}
