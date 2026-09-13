package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

/**
 * Every way deciding a claim can end — approval, rejection and the claimant's
 * own withdrawal share it, since all three settle a pending row and fail for
 * the same reasons.
 */
public sealed interface ClaimDecision {

    record Decided(PersonClaimDto claim) implements ClaimDecision {
    }

    record ClaimNotFound(Long claimId) implements ClaimDecision {
    }

    /** Already approved, rejected or withdrawn: decisions are not revisited. */
    record AlreadyDecided(PersonClaimDto claim) implements ClaimDecision {
    }

    /** Only the claimant may withdraw their own claim. */
    record NotYours() implements ClaimDecision {
    }

    /**
     * The person was linked to another account between submission and
     * approval. Rare, and the UNIQUE on user_account.person_id would catch it
     * anyway — this turns that race into a reviewable outcome.
     */
    record PersonAlreadyClaimed() implements ClaimDecision {
    }
}
