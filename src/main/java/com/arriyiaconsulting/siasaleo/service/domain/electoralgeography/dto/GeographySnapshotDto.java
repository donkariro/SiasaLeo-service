package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.SnapshotStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record GeographySnapshotDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) LocalDate referenceDate,
        @Schema(required = true, nullable = true) String sourceReference,
        @Schema(required = true, nullable = true) String sourceSha256,
        @Schema(required = true, nullable = true) Long supersedesSnapshotId,
        @Schema(required = true) SnapshotStatus status,
        @Schema(required = true) OffsetDateTime createdAt,
        @Schema(required = true, nullable = true) OffsetDateTime publishedAt) {}
