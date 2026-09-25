package com.arriyiaconsulting.siasaleo.service.domain.election.result.boundary;
import com.arriyiaconsulting.siasaleo.service.domain.election.result.control.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.result.dto.ResultDtos.*;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("contests/{contestId}/results") @RequestScoped @Produces(MediaType.APPLICATION_JSON)
public class ContestResultResource {
    @Inject private ResultReadService reads;
    @Inject private ResultPublicationService publications;
    @GET public ResultSummary results(@PathParam("contestId") long contest,@QueryParam("stage") @DefaultValue("OFFICIAL") String stage,
            @QueryParam("publicationId") Long publication,@QueryParam("areaId") Long area) { return reads.contestResults(contest,stage,publication,area); }
    @GET @Path("revisions") public List<PublicationDto> revisions(@PathParam("contestId") long contest,@QueryParam("page") @DefaultValue("0") int page,@QueryParam("size") @DefaultValue("100") int size) { return publications.publications(contest,false,page,size); }
}
