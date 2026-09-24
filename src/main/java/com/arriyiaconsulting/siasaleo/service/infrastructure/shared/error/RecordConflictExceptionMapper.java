package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class RecordConflictExceptionMapper implements ExceptionMapper<RecordConflictException> {
    @Override public Response toResponse(RecordConflictException e) { return Response.status(409).entity(Map.of("error",e.getMessage())).build(); }
}
