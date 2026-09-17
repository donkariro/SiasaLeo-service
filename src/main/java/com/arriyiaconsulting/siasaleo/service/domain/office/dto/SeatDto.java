package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Seat;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SeatDto(@Schema(required = true) Long id,
        @Schema(required = true) Long electoralAreaId,
        @Schema(required = true) Long officeId,
        @Schema(required = true, nullable = true) String description) {
    public static SeatDto from(Seat value) {
        return new SeatDto(value.getId(), value.getElectoralAreaId(), value.getOfficeId(), value.getDescription());
    }
}
