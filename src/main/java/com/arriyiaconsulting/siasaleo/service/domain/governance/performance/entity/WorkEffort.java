package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for work_effort; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "work_effort")
public class WorkEffort {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "type_code", nullable = false, length = 40)
    private String typeCode;

    @Column(name = "period_start", nullable = true)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = true)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_precision", nullable = false, length = 15)
    private DatePrecision datePrecision = DatePrecision.UNKNOWN;

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Column(name = "recorded_by_account_id", nullable = true)
    private Long recordedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public WorkEffort() {}

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String value) { typeCode = value; }

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

    public Long getRowVersion() { return rowVersion; }
}
