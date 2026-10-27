package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

/** The applicant is always the caller's own person; only the contest is chosen. */
public record StartApplicationRequest(@JsonbProperty("contest_id") @NotNull @Positive Long contestId) {}
