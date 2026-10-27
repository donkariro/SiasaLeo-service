package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotNull;

public record UpdateFormVersionRequest(
    @JsonbProperty("source") @NotNull JsonObject source,
    @JsonbProperty("row_version") @NotNull Long rowVersion
) {}
