package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.PersonEducation;
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
    public static PersonEducationDto from(PersonEducation e) {
        return new PersonEducationDto(e.getId(), e.getPerson().getId(), e.getEducationLevel().getId(), e.getEducationLevel().getLevelName(), e.getInstitution() == null ? null : e.getInstitution().getId(), e.getInstitution() == null ? null : e.getInstitution().getName(), e.getFieldOfStudy() == null ? null : e.getFieldOfStudy().getId(), e.getFieldOfStudy() == null ? null : e.getFieldOfStudy().getFieldName(), e.getFromDate(), e.getUptoDate());
    }
}
