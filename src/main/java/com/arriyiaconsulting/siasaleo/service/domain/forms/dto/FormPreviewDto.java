package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

/** answers are the sample answers with inapplicable and invalid ones removed. */
public record FormPreviewDto(
    @JsonbProperty("valid") boolean valid,
    @JsonbProperty("answers") JsonObject answers,
    @JsonbProperty("problems") List<FormProblem> problems
) {}
