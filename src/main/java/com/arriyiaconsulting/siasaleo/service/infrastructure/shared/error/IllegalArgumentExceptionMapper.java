package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Services signal domain-rule violations with IllegalArgumentException;
 * translate them to 400 rather than the container's default 500.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", String.valueOf(exception.getMessage())))
                .build();
    }
}
