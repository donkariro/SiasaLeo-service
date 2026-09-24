package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ContestDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long electionEventId,
        @Schema(required = true, nullable = true) Long seatId,
        @Schema(required = true, nullable = true) String description,
        Long officeId, Long geographySnapshotId, Long jurisdictionId) {
}
