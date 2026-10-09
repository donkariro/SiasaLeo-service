package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.boundary;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control.PoliticalPartyMediaService;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.MediaSelectionRequest;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetDetailDto;
import com.arriyiaconsulting.siasaleo.service.security.identity.control.CallerAccounts;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("party-media") @RequestScoped @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class PoliticalPartyMediaResource {
 @Inject private PoliticalPartyMediaService service;
 @Inject private CallerAccounts callers;
 @GET @Path("symbols/{id}") public DigitalAssetDetailDto symbol(@PathParam("id") long id) { return service.current(id,true); }
 @GET @Path("officials/{id}") public DigitalAssetDetailDto official(@PathParam("id") long id) { return service.current(id,false); }
 @PUT @Path("symbols/{id}") @RolesAllowed("ADMINISTRATOR")
 public DigitalAssetDetailDto symbol(@PathParam("id") long id,@NotNull @Valid MediaSelectionRequest request,@Context SecurityContext security) {
  return service.select(id,request.assetId(),callers.requireId(security),true);
 }
 @PUT @Path("officials/{id}") @RolesAllowed("ADMINISTRATOR")
 public DigitalAssetDetailDto official(@PathParam("id") long id,@NotNull @Valid MediaSelectionRequest request,@Context SecurityContext security) {
  return service.select(id,request.assetId(),callers.requireId(security),false);
 }
}
