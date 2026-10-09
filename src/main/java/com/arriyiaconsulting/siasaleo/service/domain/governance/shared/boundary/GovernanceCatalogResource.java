package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceCatalogService;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("governance/catalogs") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class GovernanceCatalogResource {
    @Inject private GovernanceCatalogService service;
    @Inject private com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService assets;
    /** Compatibility alias; the catalogue is owned by digital assets. */
    @GET @Path("asset-types")
    public List<com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetTypeDto> assetTypes() { return assets.types(); }
    @GET @Path("policy-areas") public List<PolicyAreaDto> policies() { return service.policies(); }
    @GET @Path("units") public List<UnitOfMeasureDto> units() { return service.units(); }
    @GET @Path("indicators") public List<IndicatorDto> indicators() { return service.indicators(); }
    @GET @Path("work-effort-types") public List<WorkEffortTypeDto> workTypes() { return service.workTypes(); }
    @GET @Path("participation-roles") public List<ParticipationRoleTypeDto> roles() { return service.roles(); }
    @GET @Path("evidence-relationships") public List<EvidenceRelationshipTypeDto> evidenceTypes() { return service.evidenceTypes(); }
    @POST @Path("policy-areas") @RolesAllowed("ADMINISTRATOR") public PolicyAreaDto policy(@NotNull @Valid CreatePolicyAreaRequest request) { return service.createPolicy(request); }
    @POST @Path("units") @RolesAllowed("ADMINISTRATOR") public UnitOfMeasureDto unit(@NotNull @Valid CreateUnitOfMeasureRequest request) { return service.createUnit(request); }
    @POST @Path("indicators") @RolesAllowed("ADMINISTRATOR") public IndicatorDto indicator(@NotNull @Valid CreateIndicatorRequest request) { return service.createIndicator(request); }
    @POST @Path("scopes") @RolesAllowed("ADMINISTRATOR") public MeasurementScopeDto scope(@NotNull @Valid CreateMeasurementScopeRequest request) { return service.createScope(request); }
}
