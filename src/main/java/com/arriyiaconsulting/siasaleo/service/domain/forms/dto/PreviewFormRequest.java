package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

/** Checks sample answers without saving; submit=true applies the submission rules. */
public record PreviewFormRequest(
    @JsonbProperty("answers") JsonObject answers,
    @JsonbProperty("accepted_declarations") List<String> acceptedDeclarations,
    @JsonbProperty("submit") boolean submit
) {}
