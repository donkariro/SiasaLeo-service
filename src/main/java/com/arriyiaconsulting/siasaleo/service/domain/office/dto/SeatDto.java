package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SeatDto(@Schema(required = true) Long id,
        @Schema(required = true) Long electoralAreaId,
        @Schema(required = true) Long officeId,
        @Schema(required = true, nullable = true) String description) {
}
