package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateGeographySnapshotRequest(
        @NotBlank @Size(max = 255) String name,
        LocalDate referenceDate,
        @Size(max = 2048) String sourceReference,
        @Pattern(regexp = "[0-9a-f]{64}") String sourceSha256) {}
