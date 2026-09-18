package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AreaTypeDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true) String description) {
}
