package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class RecordNotFoundExceptionMapper implements ExceptionMapper<RecordNotFoundException> {
    @Override public Response toResponse(RecordNotFoundException e) { return Response.status(404).entity(Map.of("error",e.getMessage())).build(); }
}
