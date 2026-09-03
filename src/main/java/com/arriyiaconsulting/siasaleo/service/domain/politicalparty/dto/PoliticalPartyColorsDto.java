package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A party's colours in register order. The rows carry no identity worth
 * exposing — the set is replaced wholesale — so the order of the list is the
 * whole of display_order's meaning.
 */
public record PoliticalPartyColorsDto(
        @Schema(required = true) Long politicalPartyId,
        @Schema(required = true) List<String> colors) {
}
