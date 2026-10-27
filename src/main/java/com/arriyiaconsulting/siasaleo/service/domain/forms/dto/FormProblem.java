package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.bind.annotation.JsonbProperty;

/** One reason a definition or answer document was rejected; field is null for form-level problems. */
public record FormProblem(@JsonbProperty("field") String field, @JsonbProperty("message") String message) {}
