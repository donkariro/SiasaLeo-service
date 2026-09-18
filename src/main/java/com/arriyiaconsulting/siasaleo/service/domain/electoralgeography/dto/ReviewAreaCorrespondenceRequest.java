package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import jakarta.validation.constraints.*;

public record ReviewAreaCorrespondenceRequest(
        @NotNull @Positive Long electoralAreaId,
        @NotBlank @Size(max = 2048) String evidenceReference) {}
