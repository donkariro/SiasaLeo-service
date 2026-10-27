package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotNull;

/** row_version is the form instance's, as returned with the application. */
public record SaveApplicationAnswersRequest(
    @JsonbProperty("answers") @NotNull JsonObject answers,
    @JsonbProperty("row_version") @NotNull Long rowVersion
) {}
