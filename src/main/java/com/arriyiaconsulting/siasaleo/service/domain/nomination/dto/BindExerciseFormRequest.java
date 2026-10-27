package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

public record BindExerciseFormRequest(@JsonbProperty("form_version_id") @NotNull @Positive Long formVersionId) {}
