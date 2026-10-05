package com.arriyiaconsulting.siasaleo.service.domain.office.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for officeholder_evidence; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "officeholder_evidence")
@IdClass(OfficeholderEvidenceId.class)
public class OfficeholderEvidence {
    @Id
    @Column(name = "officeholder_id", nullable = false)
    private Long officeholderId;

    @Id
    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Column(name = "locator", nullable = true, length = 255)
    private String locator;

    @Column(name = "recorded_by_account_id", nullable = false)
    private Long recordedByAccountId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public OfficeholderEvidence() {}

    public Long getOfficeholderId() { return officeholderId; }
    public void setOfficeholderId(Long value) { officeholderId = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getLocator() { return locator; }
    public void setLocator(String value) { locator = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime value) { recordedAt = value; }
}
