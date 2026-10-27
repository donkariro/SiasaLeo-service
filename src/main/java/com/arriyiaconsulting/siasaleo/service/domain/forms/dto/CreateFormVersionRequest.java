package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

/** base_version_id names the published core a tenant form extends; platform cores omit it. */
public record CreateFormVersionRequest(
    @JsonbProperty("base_version_id") @Positive Long baseVersionId,
    @JsonbProperty("source") @NotNull JsonObject source
) {}
