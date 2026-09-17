package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionEvent;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ElectionEventDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long electionCycleId,
        @Schema(required = true) LocalDate electionDate,
        @Schema(required = true) ElectionTypeDto type,
        @Schema(required = true) ElectionStatusDto status) {
    public static ElectionEventDto from(ElectionEvent event) {
        return new ElectionEventDto(event.getId(), event.getElectionCycle().getId(),
                event.getElectionDate(), ElectionTypeDto.from(event.getType()),
                ElectionStatusDto.from(event.getStatus()));
    }
}
