package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * An account's claim to be a person already in the party model (V40), and the
 * decision on it. Approving is what writes user_account.person_id, so this is
 * the only route by which an account attaches itself to a public figure.
 *
 * userAccountId and decidedBy are plain FK values rather than mapped
 * associations, mirroring UserAccount.personId: the party domain names the
 * security domain's keys without depending on its entities.
 */
@Entity
@Table(name = "person_claim")
public class PersonClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id", nullable = false, updatable = false)
    private Long userAccountId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", updatable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PersonClaimStatus status;

    @Column(length = 1000, updatable = false)
    private String evidence;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    protected PersonClaim() {
    }

    public PersonClaim(Long userAccountId, Person person, String evidence) {
        this.userAccountId = userAccountId;
        this.person = person;
        this.evidence = evidence;
        this.status = PersonClaimStatus.PENDING;
    }

    @PrePersist
    void onPersist() {
        submittedAt = OffsetDateTime.now();
    }

    /**
     * Records a reviewer's approval. Linking the account is the caller's job
     * (PersonClaimService) — this only settles the claim, so the entity never
     * reaches across into the security domain.
     */
    public void approve(Long reviewerAccountId, String note) {
        decide(PersonClaimStatus.APPROVED, reviewerAccountId, note);
    }

    public void reject(Long reviewerAccountId, String note) {
        decide(PersonClaimStatus.REJECTED, reviewerAccountId, note);
    }

    /** The claimant's own retraction, so no reviewer is recorded. */
    public void withdraw() {
        decide(PersonClaimStatus.WITHDRAWN, null, null);
    }

    private void decide(PersonClaimStatus outcome, Long reviewerAccountId, String note) {
        if (status != PersonClaimStatus.PENDING) {
            throw new IllegalStateException(
                    "Claim " + id + " is already " + status + " and cannot be decided again");
        }
        status = outcome;
        decidedBy = reviewerAccountId;
        decisionNote = note;
        decidedAt = OffsetDateTime.now();
    }

    public boolean isPending() {
        return status == PersonClaimStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Long getUserAccountId() {
        return userAccountId;
    }

    public Person getPerson() {
        return person;
    }

    public PersonClaimStatus getStatus() {
        return status;
    }

    public String getEvidence() {
        return evidence;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public Long getDecidedBy() {
        return decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
