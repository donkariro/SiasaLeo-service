package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment_delivery_link; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment_delivery_link")
public class CommitmentDeliveryLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "commitment_revision_id", nullable = false)
    private Long commitmentRevisionId;

    @Column(name = "work_effort_id", nullable = false)
    private Long workEffortId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    private DeliveryRelationship relationshipType;

    @Column(name = "rationale", nullable = false)
    private String rationale;

    @Column(name = "recorded_by_account_id", nullable = false)
    private Long recordedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public CommitmentDeliveryLink() {}

    public Long getId() { return id; }

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }

    public Long getWorkEffortId() { return workEffortId; }
    public void setWorkEffortId(Long value) { workEffortId = value; }

    public DeliveryRelationship getRelationshipType() { return relationshipType; }
    public void setRelationshipType(DeliveryRelationship value) { relationshipType = value; }

    public String getRationale() { return rationale; }
    public void setRationale(String value) { rationale = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
