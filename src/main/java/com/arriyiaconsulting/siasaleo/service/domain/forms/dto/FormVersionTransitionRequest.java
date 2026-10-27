package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotNull;

/** Publish or retire the version as last read. */
public record FormVersionTransitionRequest(@JsonbProperty("row_version") @NotNull Long rowVersion) {}
