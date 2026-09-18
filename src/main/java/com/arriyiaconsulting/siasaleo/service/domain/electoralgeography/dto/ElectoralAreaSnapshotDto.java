package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ElectoralAreaSnapshotDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long snapshotId,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) String areaCode,
        @Schema(required = true) String areaType,
        @Schema(required = true, nullable = true) Long parentId,
        @Schema(required = true, nullable = true) String sourceRecordReference) {}
