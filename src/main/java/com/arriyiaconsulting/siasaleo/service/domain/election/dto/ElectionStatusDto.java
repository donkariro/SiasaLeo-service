package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ElectionStatusDto(@Schema(required = true) Long id,
        @Schema(required = true) String statusName,
        @Schema(required = true, nullable = true) String description) {
    public static ElectionStatusDto from(ElectionStatus value) {
        return new ElectionStatusDto(value.getId(), value.getStatusName(), value.getDescription());
    }
}
