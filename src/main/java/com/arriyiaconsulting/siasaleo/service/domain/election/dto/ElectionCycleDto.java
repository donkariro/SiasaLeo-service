package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionCycle;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ElectionCycleDto(@Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true) int fromYear,
        @Schema(required = true) int uptoYear,
        @Schema(required = true) String status) {
    public static ElectionCycleDto from(ElectionCycle value) {
        return new ElectionCycleDto(value.getId(), value.getName(), value.getFromYear(), value.getUptoYear(), value.getStatus());
    }
}
