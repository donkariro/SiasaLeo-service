package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import jakarta.validation.constraints.*;

public record CreateInstitutionRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String registrationNumber,
        @NotNull @Positive Long institutionTypeId) {}
