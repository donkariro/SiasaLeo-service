package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.FieldOfStudy;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record FieldOfStudyDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) Long parentId,
        @Schema(required = true, nullable = true) String description) {
    public static FieldOfStudyDto from(FieldOfStudy e) {
        return new FieldOfStudyDto(e.getId(), e.getFieldName(), e.getParent() == null ? null : e.getParent().getId(), e.getDescription());
    }
}
