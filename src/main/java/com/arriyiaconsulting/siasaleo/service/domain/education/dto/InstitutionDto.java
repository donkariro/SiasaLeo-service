package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.EducationalInstitution;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record InstitutionDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) String registrationNumber,
        @Schema(required = true) Long institutionTypeId,
        @Schema(required = true) String institutionTypeName) {
    public static InstitutionDto from(EducationalInstitution e) {
        return new InstitutionDto(e.getId(), e.getName(), e.getRegistrationNumber(), e.getInstitutionType().getId(), e.getInstitutionType().getTypeName());
    }
}
