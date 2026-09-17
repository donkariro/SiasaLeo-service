package com.arriyiaconsulting.siasaleo.service.domain.election.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.election.control.ElectionService;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("elections")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ElectionCatalogResource {
    @Inject private ElectionService service;

    @GET @Path("cycles")
    public List<ElectionCycleDto> cycles() { return service.cycles(); }

    @GET @Path("cycles/{id}")
    public ElectionCycleDto cycle(@PathParam("id") Long id) {
        return service.findCycle(id).orElseThrow(NotFoundException::new);
    }

    @GET @Path("types")
    public List<ElectionTypeDto> types() { return service.types(); }

    @GET @Path("statuses")
    public List<ElectionStatusDto> statuses() { return service.statuses(); }
}
