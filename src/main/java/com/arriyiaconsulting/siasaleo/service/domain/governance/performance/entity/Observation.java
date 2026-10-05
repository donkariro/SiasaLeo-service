package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for performance_observation; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "performance_observation")
public class Observation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "indicator_id", nullable = true)
    private Long indicatorId;

    @Column(name = "actual_value", nullable = true, precision = 20, scale = 6)
    private BigDecimal actualValue;

    @Column(name = "qualitative_result", nullable = true)
    private String qualitativeResult;

    @Column(name = "officeholder_id", nullable = true)
    private Long officeholderId;

    @Column(name = "work_effort_id", nullable = true)
    private Long workEffortId;

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Column(name = "period_start", nullable = true)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = true)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_precision", nullable = false, length = 15)
    private DatePrecision datePrecision = DatePrecision.UNKNOWN;

    @Column(name = "source_reference", nullable = true, length = 2048)
    private String sourceReference;

    @Column(name = "recorded_by_account_id", nullable = true)
    private Long recordedByAccountId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 15)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(name = "published_at", nullable = true, insertable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "legacy_measurement_id", nullable = true)
    private Long legacyMeasurementId;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public Observation() {}

    public Long getId() { return id; }

    public Long getIndicatorId() { return indicatorId; }
    public void setIndicatorId(Long value) { indicatorId = value; }

    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal value) { actualValue = value; }

    public String getQualitativeResult() { return qualitativeResult; }
    public void setQualitativeResult(String value) { qualitativeResult = value; }

    public Long getOfficeholderId() { return officeholderId; }
    public void setOfficeholderId(Long value) { officeholderId = value; }

    public Long getWorkEffortId() { return workEffortId; }
    public void setWorkEffortId(Long value) { workEffortId = value; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long value) { scopeId = value; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }

    public DatePrecision getDatePrecision() { return datePrecision; }
    public void setDatePrecision(DatePrecision value) { datePrecision = value; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { sourceReference = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime value) { recordedAt = value; }

    public PublicationStatus getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(PublicationStatus value) { publicationStatus = value; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }

    public Long getLegacyMeasurementId() { return legacyMeasurementId; }
    public void setLegacyMeasurementId(Long value) { legacyMeasurementId = value; }

    public Long getRowVersion() { return rowVersion; }
}
