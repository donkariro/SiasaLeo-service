package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record VoterRegistrationDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long personId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) Long registrationCenterId,
        @Schema(required = true) String registrationCenterName,
        @Schema(required = true) LocalDate registrationDate,
        @Schema(required = true) String status) {
}
