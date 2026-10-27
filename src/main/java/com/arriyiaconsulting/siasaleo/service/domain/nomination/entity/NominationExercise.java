package com.arriyiaconsulting.siasaleo.service.domain.nomination.entity;

import jakarta.persistence.*;
import java.time.*;

/** JPA mapping for nomination_exercise (V60): one party's application round for an election event. */
@Entity
@Table(name = "nomination_exercise")
public class NominationExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "political_party_id", nullable = false, updatable = false)
    private Long politicalPartyId;

    @Column(name = "election_event_id", nullable = false, updatable = false)
    private Long electionEventId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ExerciseStatus status = ExerciseStatus.DRAFT;

    @Column(name = "opens_at", nullable = false)
    private OffsetDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private OffsetDateTime closesAt;

    @Column(name = "created_by_account_id", updatable = false)
    private Long createdByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    protected NominationExercise() {}

    public NominationExercise(Long politicalPartyId, Long electionEventId, String title,
                              OffsetDateTime opensAt, OffsetDateTime closesAt, Long createdByAccountId) {
        this.politicalPartyId = politicalPartyId;
        this.electionEventId = electionEventId;
        this.title = title;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.createdByAccountId = createdByAccountId;
    }

    /** Aspirants may apply, edit and submit only while this holds. */
    public boolean accepting(OffsetDateTime now) {
        return status == ExerciseStatus.OPEN && !now.isBefore(opensAt) && now.isBefore(closesAt);
    }
    public void open() {
        if (status != ExerciseStatus.DRAFT) throw new IllegalArgumentException("Only a draft exercise can be opened");
        status = ExerciseStatus.OPEN;
    }
    public void close() {
        if (status != ExerciseStatus.OPEN) throw new IllegalArgumentException("Only an open exercise can be closed");
        status = ExerciseStatus.CLOSED;
    }

    public Long getId() { return id; }
    public Long getPoliticalPartyId() { return politicalPartyId; }
    public Long getElectionEventId() { return electionEventId; }
    public String getTitle() { return title; }
    public ExerciseStatus getStatus() { return status; }
    public OffsetDateTime getOpensAt() { return opensAt; }
    public OffsetDateTime getClosesAt() { return closesAt; }
    public Long getCreatedByAccountId() { return createdByAccountId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Long getRowVersion() { return rowVersion; }
}
