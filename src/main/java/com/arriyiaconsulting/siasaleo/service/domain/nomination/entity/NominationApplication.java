package com.arriyiaconsulting.siasaleo.service.domain.nomination.entity;

import jakarta.persistence.*;
import java.time.*;

/**
 * JPA mapping for nomination_application (V60): an aspirant's application to
 * a party for one contest. The answers live in a forms instance linked
 * through nomination_application_form.
 */
@Entity
@Table(name = "nomination_application")
public class NominationApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "exercise_id", nullable = false, updatable = false)
    private Long exerciseId;

    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    @Column(name = "contest_id", nullable = false, updatable = false)
    private Long contestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "created_by_account_id", updatable = false)
    private Long createdByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    protected NominationApplication() {}

    public NominationApplication(Long exerciseId, Long personId, Long contestId, Long createdByAccountId) {
        this.exerciseId = exerciseId;
        this.personId = personId;
        this.contestId = contestId;
        this.createdByAccountId = createdByAccountId;
    }

    /** Records the latest submitted revision; corrections resubmit while the exercise accepts them. */
    public void submitted(OffsetDateTime at) {
        requireLive();
        status = ApplicationStatus.SUBMITTED;
        submittedAt = at;
    }
    public void withdraw(OffsetDateTime at) {
        requireLive();
        status = ApplicationStatus.WITHDRAWN;
        withdrawnAt = at;
    }
    public void requireLive() {
        if (status == ApplicationStatus.WITHDRAWN) throw new IllegalArgumentException("The application has been withdrawn");
    }

    public Long getId() { return id; }
    public Long getExerciseId() { return exerciseId; }
    public Long getPersonId() { return personId; }
    public Long getContestId() { return contestId; }
    public ApplicationStatus getStatus() { return status; }
    public Long getCreatedByAccountId() { return createdByAccountId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public OffsetDateTime getWithdrawnAt() { return withdrawnAt; }
    public Long getRowVersion() { return rowVersion; }
}
