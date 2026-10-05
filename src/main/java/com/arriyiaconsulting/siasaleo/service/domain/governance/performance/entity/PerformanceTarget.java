package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for performance_target; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "performance_target")
public class PerformanceTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "officeholder_id", nullable = false)
    private Long officeholderId;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "baseline_value", nullable = true, precision = 20, scale = 6)
    private BigDecimal baselineValue;

    @Column(name = "target_value", nullable = false, precision = 20, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "source_reference", nullable = false, length = 2048)
    private String sourceReference;

    @Column(name = "notes", nullable = true)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public PerformanceTarget() {}

    public Long getId() { return id; }

    public Long getOfficeholderId() { return officeholderId; }
    public void setOfficeholderId(Long value) { officeholderId = value; }

    public Long getIndicatorId() { return indicatorId; }
    public void setIndicatorId(Long value) { indicatorId = value; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }

    public BigDecimal getBaselineValue() { return baselineValue; }
    public void setBaselineValue(BigDecimal value) { baselineValue = value; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal value) { targetValue = value; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { sourceReference = value; }

    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long value) { scopeId = value; }

    public Long getRowVersion() { return rowVersion; }
}
