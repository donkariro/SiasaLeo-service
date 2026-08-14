package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control.ElectoralAreaService;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.AreaTypeDto;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("area-types")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AreaTypeResource {

    @Inject
    private ElectoralAreaService service;

    @GET
    public List<AreaTypeDto> list() {
        return service.findAllAreaTypes();
    }
}
