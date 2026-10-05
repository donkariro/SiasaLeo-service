package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for work_effort_participation; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "work_effort_participation")
public class WorkEffortParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "work_effort_id", nullable = false)
    private Long workEffortId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "role_code", nullable = false, length = 40)
    private String roleCode;

    @Column(name = "officeholder_id", nullable = true)
    private Long officeholderId;

    @Column(name = "period_start", nullable = true)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = true)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_precision", nullable = false, length = 15)
    private DatePrecision datePrecision = DatePrecision.UNKNOWN;

    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Column(name = "locator", nullable = true, length = 255)
    private String locator;

    @Column(name = "recorded_by_account_id", nullable = false)
    private Long recordedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public WorkEffortParticipation() {}

    public Long getId() { return id; }

    public Long getWorkEffortId() { return workEffortId; }
    public void setWorkEffortId(Long value) { workEffortId = value; }

    public Long getPartyId() { return partyId; }
    public void setPartyId(Long value) { partyId = value; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String value) { roleCode = value; }

    public Long getOfficeholderId() { return officeholderId; }
    public void setOfficeholderId(Long value) { officeholderId = value; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { periodStart = value; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { periodEnd = value; }

    public DatePrecision getDatePrecision() { return datePrecision; }
    public void setDatePrecision(DatePrecision value) { datePrecision = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getLocator() { return locator; }
    public void setLocator(String value) { locator = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
