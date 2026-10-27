package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotNull;

/** Changes the record as last read; a stale row_version is a conflict. */
public record NominationTransitionRequest(@JsonbProperty("row_version") @NotNull Long rowVersion) {}
