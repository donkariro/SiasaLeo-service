package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Claims an existing person. The claimant is never named here — it is the
 * authenticated account, resolved at the boundary — so this payload cannot be
 * used to file a claim on someone else's behalf.
 */
public record SubmitClaimRequest(
        @NotNull Long personId,
        @Size(max = 1000) String evidence) {
}
