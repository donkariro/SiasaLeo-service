package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record FieldOfStudyDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) Long parentId,
        @Schema(required = true, nullable = true) String description) {
}
