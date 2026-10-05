package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for performance_record; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "performance_record")
public class ClaimRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "officeholder_id", nullable = false)
    private Long officeholderId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "period_start", nullable = true)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = true)
    private LocalDate periodEnd;

    @Column(name = "source_reference", nullable = true, length = 2048)
    private String sourceReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber = 1;

    @Column(name = "claimant_party_id", nullable = true)
    private Long claimantPartyId;

    @Column(name = "recorded_by_account_id", nullable = true)
    private Long recordedByAccountId;

    @Column(name = "work_effort_id", nullable = true)
    private Long workEffortId;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_precision", nullable = false, length = 15)
    private DatePrecision datePrecision = DatePrecision.DAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 15)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(name = "published_at", nullable = true, insertable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public ClaimRevision() {}

    public Long getId() { return id; }

    public Long getOfficeholderId() { return officeholderId; }
    public void setOfficeholderId(Long value) { officeholderId = value; }

    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { sourceReference = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }

    public Long getClaimId() { return claimId; }
    public void setClaimId(Long value) { claimId = value; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer value) { revisionNumber = value; }

    public Long getClaimantPartyId() { return claimantPartyId; }
    public void setClaimantPartyId(Long value) { claimantPartyId = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public Long getWorkEffortId() { return workEffortId; }
    public void setWorkEffortId(Long value) { workEffortId = value; }

    public DatePrecision getDatePrecision() { return datePrecision; }
    public void setDatePrecision(DatePrecision value) { datePrecision = value; }

    public PublicationStatus getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(PublicationStatus value) { publicationStatus = value; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long value) { scopeId = value; }

    public Long getRowVersion() { return rowVersion; }
}
