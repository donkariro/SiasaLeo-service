package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A person offered as the target of a claim. Carries only what a claimant
 * needs to recognize themselves in the list — date of birth is held back,
 * since this endpoint is readable by any signed-in user and picking your own
 * row out of a name search never needs it.
 */
public record ClaimablePersonDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName) {
}
