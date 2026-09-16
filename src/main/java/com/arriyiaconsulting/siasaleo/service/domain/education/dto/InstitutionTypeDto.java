package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.EducationalInstitutionType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record InstitutionTypeDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) String description) {
    public static InstitutionTypeDto from(EducationalInstitutionType e) {
        return new InstitutionTypeDto(e.getId(), e.getTypeName(), e.getDescription());
    }
}
