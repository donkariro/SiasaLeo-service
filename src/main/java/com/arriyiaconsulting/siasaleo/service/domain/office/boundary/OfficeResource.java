package com.arriyiaconsulting.siasaleo.service.domain.office.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.office.control.OfficeService;
import com.arriyiaconsulting.siasaleo.service.domain.office.dto.OfficeDto;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("offices")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class OfficeResource {

    @Inject
    private OfficeService service;

    /** All offices for selection, including deputy offices. */
    @GET
    public List<OfficeDto> list() {
        return service.findAll();
    }

    @GET
    @Path("{id}")
    public OfficeDto get(@PathParam("id") Long id) {
        return service.findById(id).orElseThrow(NotFoundException::new);
    }
}
