package com.arriyiaconsulting.siasaleo.service.domain.party.boundary;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PartyMediaService;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetDetailDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
@Path("parties") @RequestScoped @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class PartyMediaResource {
 @Inject private PartyMediaService service;
 @Inject private CallerAccounts callers;
 private Long actor(SecurityContext security) { return security!=null && security.getUserPrincipal()!=null ? callers.requireId(security):null; }
 @GET @Path("{id}/media/{purpose}")
 public DigitalAssetDetailDto get(@PathParam("id") long id,@PathParam("purpose") String purpose,@Context SecurityContext security) {
  return service.current(id,purpose,actor(security));
 }
 @PUT @Path("{id}/media/{purpose}") @RolesAllowed({"USER","ADMINISTRATOR"})
 public DigitalAssetDetailDto select(@PathParam("id") long id,@PathParam("purpose") String purpose,
  @NotNull @Valid MediaSelectionRequest request,@Context SecurityContext security) {
  return service.select(id,purpose,request.assetId(),callers.requireId(security));
 }
 @DELETE @Path("{id}/media/{purpose}") @RolesAllowed({"USER","ADMINISTRATOR"})
 public void remove(@PathParam("id") long id,@PathParam("purpose") String purpose,@Context SecurityContext security) {
  service.remove(id,purpose,callers.requireId(security));
 }
 @PUT @Path("{id}/media-managers/{account}") @RolesAllowed("ADMINISTRATOR")
 public void manager(@PathParam("id") long id,@PathParam("account") long account,@Context SecurityContext security) {
  service.manager(id,account,callers.requireId(security),false);
 }
 @DELETE @Path("{id}/media-managers/{account}") @RolesAllowed("ADMINISTRATOR")
 public void revokeManager(@PathParam("id") long id,@PathParam("account") long account,@Context SecurityContext security) {
  service.manager(id,account,callers.requireId(security),true);
 }
 @PUT @Path("{id}/attachments/{purpose}") @RolesAllowed({"USER","ADMINISTRATOR"})
 public PersonDigitalAssetDto attach(@PathParam("id") long id,@PathParam("purpose") String purpose,
  @NotNull @Valid MediaAttachmentRequest request,@Context SecurityContext security) {
  return service.attach(id,purpose,request,callers.requireId(security));
 }
 @GET @Path("{id}/attachments")
 public List<PersonDigitalAssetDto> attachments(@PathParam("id") long id,@Context SecurityContext security,
  @QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("25") int size) {
  return service.attachments(id,actor(security),page,size);
 }
 @DELETE @Path("{id}/attachments/{purpose}/{version}") @RolesAllowed({"USER","ADMINISTRATOR"})
 public void detach(@PathParam("id") long id,@PathParam("purpose") String purpose,@PathParam("version") long version,@Context SecurityContext security) {
  service.detach(id,version,purpose,callers.requireId(security));
 }
}
