package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for performance_assessment; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "performance_assessment")
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "record_id", nullable = true)
    private Long recordId;

    @Column(name = "commitment_revision_id", nullable = true)
    private Long commitmentRevisionId;

    @Column(name = "commitment_target_id", nullable = true)
    private Long commitmentTargetId;

    @Column(name = "observation_id", nullable = true)
    private Long observationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = true, length = 25)
    private DeliveryStatus deliveryStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 25)
    private EvidenceStatus evidenceStatus;

    @Column(name = "criteria", nullable = false)
    private String criteria;

    @Column(name = "rationale", nullable = false)
    private String rationale;

    @Column(name = "reviewed_by_account_id", nullable = false)
    private Long reviewedByAccountId;

    @Column(name = "assessed_at", nullable = false, updatable = false)
    private OffsetDateTime assessedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 15)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(name = "published_at", nullable = true, insertable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public Assessment() {}

    public Long getId() { return id; }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long value) { recordId = value; }

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }

    public Long getCommitmentTargetId() { return commitmentTargetId; }
    public void setCommitmentTargetId(Long value) { commitmentTargetId = value; }

    public Long getObservationId() { return observationId; }
    public void setObservationId(Long value) { observationId = value; }

    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus value) { deliveryStatus = value; }

    public EvidenceStatus getEvidenceStatus() { return evidenceStatus; }
    public void setEvidenceStatus(EvidenceStatus value) { evidenceStatus = value; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String value) { criteria = value; }

    public String getRationale() { return rationale; }
    public void setRationale(String value) { rationale = value; }

    public Long getReviewedByAccountId() { return reviewedByAccountId; }
    public void setReviewedByAccountId(Long value) { reviewedByAccountId = value; }

    public OffsetDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(OffsetDateTime value) { assessedAt = value; }

    public PublicationStatus getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(PublicationStatus value) { publicationStatus = value; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }

    public Long getRowVersion() { return rowVersion; }
}
