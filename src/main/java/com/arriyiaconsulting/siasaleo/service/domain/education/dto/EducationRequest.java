package com.arriyiaconsulting.siasaleo.service.domain.education.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** A study episode; a null end date means study is ongoing. */
public record EducationRequest(
        @NotNull @Positive Long educationLevelId,
        @Positive Long institutionId,
        @Positive Long fieldOfStudyId,
        LocalDate fromDate,
        LocalDate uptoDate) {}
