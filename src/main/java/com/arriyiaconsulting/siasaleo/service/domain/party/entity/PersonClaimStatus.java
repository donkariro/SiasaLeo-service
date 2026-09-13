package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

/**
 * Lifecycle of a person claim (V40). Only PENDING is constrained to one row
 * per account and one per person; the decided statuses accumulate as history.
 * WITHDRAWN is the claimant's own retraction, REJECTED a reviewer's refusal —
 * kept apart because only the latter is a signal when the same account claims
 * the same person again.
 */
public enum PersonClaimStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
