package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for claim_evidence; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "claim_evidence")
@IdClass(ClaimEvidenceId.class)
public class ClaimEvidence {
    @Id
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Id
    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Id
    @Column(name = "relationship_code", nullable = false, length = 30)
    private String relationshipCode;

    @Column(name = "locator", nullable = true, length = 255)
    private String locator;

    @Column(name = "caption", nullable = true)
    private String caption;

    public ClaimEvidence() {}

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long value) { recordId = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getRelationshipCode() { return relationshipCode; }
    public void setRelationshipCode(String value) { relationshipCode = value; }

    public String getLocator() { return locator; }
    public void setLocator(String value) { locator = value; }

    public String getCaption() { return caption; }
    public void setCaption(String value) { caption = value; }
}
