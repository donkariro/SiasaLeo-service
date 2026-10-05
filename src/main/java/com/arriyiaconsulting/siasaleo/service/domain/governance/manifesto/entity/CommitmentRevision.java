package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment_revision; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment_revision")
public class CommitmentRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "commitment_id", nullable = false)
    private Long commitmentId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(name = "wording", nullable = false)
    private String wording;

    @Column(name = "conditions", nullable = true)
    private String conditions;

    @Column(name = "period_start", nullable = true)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = true)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_precision", nullable = false, length = 15)
    private DatePrecision datePrecision = DatePrecision.UNKNOWN;

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Column(name = "recorded_by_account_id", nullable = false)
    private Long recordedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "sealed_at", nullable = true)
    private OffsetDateTime sealedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public CommitmentRevision() {}

    public Long getId() { return id; }

    public Long getCommitmentId() { return commitmentId; }
    public void setCommitmentId(Long value) { commitmentId = value; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer value) { revisionNumber = value; }

    public String getWording() { return wording; }
    public void setWording(String value) { wording = value; }

    public String getConditions() { return conditions; }
    public void setConditions(String value) { conditions = value; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }

    public DatePrecision getDatePrecision() { return datePrecision; }
    public void setDatePrecision(DatePrecision value) { datePrecision = value; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long value) { scopeId = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }

    public OffsetDateTime getSealedAt() { return sealedAt; }
    public void setSealedAt(OffsetDateTime value) { sealedAt = value; }

    public Long getRowVersion() { return rowVersion; }
}
