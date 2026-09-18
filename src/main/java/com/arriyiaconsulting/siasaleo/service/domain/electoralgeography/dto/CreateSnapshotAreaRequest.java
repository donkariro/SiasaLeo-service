package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import jakarta.validation.constraints.*;

public record CreateSnapshotAreaRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String areaCode,
        @NotBlank String areaType,
        @Positive Long parentId,
        @Size(max = 1024) String sourceRecordReference) {}
