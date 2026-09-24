package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

/**
 * The election statuses V28 seeds into election_status, and the lifecycle
 * between them. The table stays the source of ids and descriptions; this enum
 * is what code compares against, so a misspelt status is a compile error
 * rather than a comparison that silently never matches.
 * <p>
 * The switch in {@link #canChangeTo} is exhaustive, so adding a status here
 * forces a decision about its transitions. A status seeded in the database
 * without a constant here is a deployment error, reported by {@link #of}.
 */
public enum ElectionStatusName {
    SCHEDULED, ONGOING, COMPLETED, NULLIFIED, CANCELLED;

    public boolean canChangeTo(ElectionStatusName target) {
        return switch (this) {
            case SCHEDULED -> target == ONGOING || target == CANCELLED;
            case ONGOING -> target == COMPLETED || target == NULLIFIED;
            // A declared result can still be annulled by a court.
            case COMPLETED -> target == NULLIFIED;
            case NULLIFIED, CANCELLED -> false;
        };
    }

    public static ElectionStatusName of(String statusName) {
        try {
            return valueOf(statusName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Election status " + statusName
                    + " is seeded but has no ElectionStatusName; add it and its transitions", e);
        }
    }
}
