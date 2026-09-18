package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import java.time.OffsetDateTime;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AreaCorrespondenceDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long electoralAreaId,
        @Schema(required = true) Long areaSnapshotId,
        @Schema(required = true) String evidenceReference,
        @Schema(required = true, nullable = true) OffsetDateTime reviewedAt) {}
