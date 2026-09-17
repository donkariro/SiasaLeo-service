package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ContestDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long electionEventId,
        @Schema(required = true) Long seatId,
        @Schema(required = true, nullable = true) String description) {
    public static ContestDto from(Contest contest) {
        return new ContestDto(contest.getId(), contest.getElectionEventId(),
                contest.getSeatId(), contest.getDescription());
    }
}
