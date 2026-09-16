package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Office;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Office reference data for selection controls and display. */
public record OfficeDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true) String abbreviation,
        @Schema(required = true, nullable = true) String description,
        @Schema(required = true, enumeration = {"National", "County"}) String level) {

    public static OfficeDto from(Office office) {
        return new OfficeDto(office.getId(), office.getName(), office.getAbbreviation(),
                office.getDescription(), office.getLevel());
    }
}
