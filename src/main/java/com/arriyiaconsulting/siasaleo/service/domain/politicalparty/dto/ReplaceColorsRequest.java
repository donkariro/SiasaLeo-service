package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Replaces a party's colours with the ones given, in the order given. An empty
 * list clears them, which the register needs: it lists no colours for some
 * parties. Colours are free text — V20 carried tokens like 'Colourless' across
 * verbatim — and are not deduplicated.
 */
public record ReplaceColorsRequest(
        @NotNull List<@NotBlank @Size(max = 100) String> colors) {
}
