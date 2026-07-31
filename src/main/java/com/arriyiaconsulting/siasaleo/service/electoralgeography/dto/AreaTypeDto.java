package com.arriyiaconsulting.siasaleo.service.electoralgeography.dto;

import com.arriyiaconsulting.siasaleo.service.electoralgeography.entity.AreaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AreaTypeDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true) String description) {

    public static AreaTypeDto from(AreaType type) {
        return new AreaTypeDto(type.getId(), type.getName(), type.getDescription());
    }
}
