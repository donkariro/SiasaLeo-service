package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import jakarta.validation.constraints.*;

public record UpdateSnapshotAreaRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String areaCode,
        @Size(max = 1024) String sourceRecordReference) {}
