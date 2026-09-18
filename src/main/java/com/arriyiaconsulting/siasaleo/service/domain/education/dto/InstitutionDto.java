package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record InstitutionDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) String registrationNumber,
        @Schema(required = true) Long institutionTypeId,
        @Schema(required = true) String institutionTypeName) {
}
