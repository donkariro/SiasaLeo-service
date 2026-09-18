package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record PersonEducationDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long personId,
        @Schema(required = true) Long educationLevelId,
        @Schema(required = true) String educationLevelName,
        @Schema(required = true, nullable = true) Long institutionId,
        @Schema(required = true, nullable = true) String institutionName,
        @Schema(required = true, nullable = true) Long fieldOfStudyId,
        @Schema(required = true, nullable = true) String fieldOfStudyName,
        @Schema(required = true, nullable = true) LocalDate fromDate,
        @Schema(required = true, nullable = true) LocalDate uptoDate) {
}
