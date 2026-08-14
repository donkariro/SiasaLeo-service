package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ElectoralAreaDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true) String areaCode,
        @Schema(required = true) String areaType,
        // Null only for the WORLD root; still listed as required so generated
        // client types get `number | null` rather than an optional property.
        @Schema(required = true, nullable = true) Long parentId) {

    public static ElectoralAreaDto from(ElectoralArea area) {
        return new ElectoralAreaDto(area.getId(), area.getName(), area.getAreaCode(),
                area.getAreaType().getName(), area.getParentId());
    }
}
