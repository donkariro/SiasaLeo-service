package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ElectionTypeDto(@Schema(required = true) Long id,
        @Schema(required = true) String typeName,
        @Schema(required = true, nullable = true) String description) {
    public static ElectionTypeDto from(ElectionType value) {
        return new ElectionTypeDto(value.getId(), value.getTypeName(), value.getDescription());
    }
}
