package com.arriyiaconsulting.siasaleo.service.domain.office.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.office.control.SeatService;
import com.arriyiaconsulting.siasaleo.service.domain.office.dto.*;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("seats")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatResource {
    @Inject private SeatService service;

    @GET
    public List<SeatDto> list(@QueryParam("officeId") Long officeId,
            @QueryParam("electoralAreaId") Long areaId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return service.search(officeId, areaId, page, size);
    }

    @GET @Path("{id}")
    public SeatDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }
}
