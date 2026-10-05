package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment_target; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment_target")
public class CommitmentTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "commitment_revision_id", nullable = false)
    private Long commitmentRevisionId;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "baseline_value", nullable = true, precision = 20, scale = 6)
    private BigDecimal baselineValue;

    @Column(name = "target_value", nullable = false, precision = 20, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    public CommitmentTarget() {}

    public Long getId() { return id; }

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }

    public Long getIndicatorId() { return indicatorId; }
    public void setIndicatorId(Long value) { indicatorId = value; }

    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long value) { scopeId = value; }

    public BigDecimal getBaselineValue() { return baselineValue; }
    public void setBaselineValue(BigDecimal value) { baselineValue = value; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal value) { targetValue = value; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }
}
