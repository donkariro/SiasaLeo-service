package com.arriyiaconsulting.siasaleo.service.domain.election.boundary;
import com.arriyiaconsulting.siasaleo.service.domain.election.control.RegisterAssignmentService;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.RegisterAssignmentDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegisterService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("election-events/{eventId}/voter-register") @RequestScoped
@Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ElectionRegisterResource {
    @Inject private RegisterAssignmentService assignments;
    @Inject private VoterRegisterService registers;
    private AssignmentDto current(long event) { return assignments.current(event).orElseThrow(NotFoundException::new); }
    @GET public RegisterSummary summary(@PathParam("eventId") long event,@QueryParam("areaId") Long area) { return registers.summary(current(event).registerId(),area,false); }
    @GET @Path("counts") public List<RegisterCount> counts(@PathParam("eventId") long event,@QueryParam("areaId") Long area,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return registers.counts(current(event).registerId(),area,false,page,size); }
    @GET @Path("assignment") public AssignmentDto assignment(@PathParam("eventId") long event) { return current(event); }
    @POST @Path("assignments") @RolesAllowed("ADMINISTRATOR")
    public AssignmentDto assign(@PathParam("eventId") long event,@NotNull @Valid AssignmentRequest r) { return assignments.assign(event,r); }
    @GET @Path("assignments") public List<AssignmentDto> assignments(@PathParam("eventId") long event,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return assignments.assignments(event,page,size); }
}
