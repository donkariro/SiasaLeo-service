package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class RecordAccessDeniedExceptionMapper implements ExceptionMapper<RecordAccessDeniedException> {
    public Response toResponse(RecordAccessDeniedException error) {
        return Response.status(403).entity(Map.of("error",error.getMessage())).build();
    }
}
