package com.arriyiaconsulting.siasaleo.service.domain.forms.boundary;

import com.arriyiaconsulting.siasaleo.service.domain.forms.control.InvalidFormException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.util.Map;

/** 400 with the full problem list, so a form author or applicant can fix everything in one pass. */
@Provider
public class InvalidFormExceptionMapper implements ExceptionMapper<InvalidFormException> {
    @Override
    public Response toResponse(InvalidFormException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", e.getMessage(), "problems", e.getProblems()))
                .build();
    }
}
