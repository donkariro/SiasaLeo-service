package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class RecordConflictExceptionMapper implements ExceptionMapper<RecordConflictException> {
    @Override
    public Response toResponse(RecordConflictException e) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", e.getMessage()))
                .build();
    }
}
