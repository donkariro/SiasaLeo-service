package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class RecordAccessDeniedExceptionMapper implements ExceptionMapper<RecordAccessDeniedException> {
    @Override
    public Response toResponse(RecordAccessDeniedException error) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", error.getMessage()))
                .build();
    }
}
