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

@Path("digital-assets") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class DigitalAssetResource {
    @Inject private DigitalAssetService service;
    @Inject private CallerAccounts callers;
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
    public DigitalAssetDetailDto read(@PathParam("id") long id) { return service.read(id, null); }
    @GET @Path("me/versions/{id}") @RolesAllowed({"USER", "ADMINISTRATOR"})
    public DigitalAssetDetailDto draft(@PathParam("id") long id, @Context SecurityContext security) { return service.read(id, callers.requireId(security)); }
}
