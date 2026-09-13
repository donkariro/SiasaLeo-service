package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

/**
 * Every way submitting a claim can end. Follows RegistrationService's idiom
 * rather than VoterRegistrationService's exceptions: a claim is a user-facing
 * request with several legitimate refusals that each need their own HTTP
 * status, and the boundary switches over this exhaustively so a new variant
 * is a compile error until every caller maps it.
 */
public sealed interface ClaimResult {

    record Submitted(PersonClaimDto claim) implements ClaimResult {
    }

    /** The claimant's account is not ACTIVE, so it cannot claim anyone yet. */
    record AccountUnavailable() implements ClaimResult {
    }

    record PersonNotFound(Long personId) implements ClaimResult {
    }

    /** This account already resolves to a person; it has nobody left to claim. */
    record AlreadyLinked(Long personId) implements ClaimResult {
    }

    /** Another account holds this person — the UNIQUE on user_account.person_id. */
    record PersonAlreadyClaimed() implements ClaimResult {
    }

    /** One open claim per account, and one per person (V40). */
    record ClaimAlreadyOpen(PersonClaimDto existing) implements ClaimResult {
    }
}
