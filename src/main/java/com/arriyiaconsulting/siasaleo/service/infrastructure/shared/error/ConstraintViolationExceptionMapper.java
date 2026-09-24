package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Bean Validation failures (@Valid on request DTOs) would otherwise surface in
 * the container's default format; map them to the same {"error": ...} shape as
 * IllegalArgumentExceptionMapper, extended with per-field violations so client
 * error handling can be written once.
 */
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<Map<String, String>> violations = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", fieldOf(violation),
                        "message", String.valueOf(violation.getMessage())))
                .sorted(Comparator.comparing(v -> v.get("field")))
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "Validation failed", "violations", violations))
                .build();
    }

    // The full path of a resource-method violation is e.g. "create.arg0.name";
    // only the property nodes ("name") mean anything to the client. List
    // positions are kept ("votes[3].voteCount") so a bulk import says which
    // row failed. A node inside a list carries the position of the element
    // it sits in, so the index is written before the node's own name.
    static String fieldOf(ConstraintViolation<?> violation) {
        StringBuilder field = new StringBuilder();
        for (Path.Node node : violation.getPropertyPath()) {
            boolean property = node.getKind() == ElementKind.PROPERTY && node.getName() != null;
            boolean element = node.getKind() == ElementKind.CONTAINER_ELEMENT;
            if ((property || element) && field.length() > 0 && node.isInIterable() && node.getIndex() != null) {
                field.append('[').append(node.getIndex()).append(']');
            }
            if (property) {
                if (field.length() > 0) field.append('.');
                field.append(node.getName());
            }
        }
        return field.length() == 0 ? String.valueOf(violation.getPropertyPath()) : field.toString();
    }
}
