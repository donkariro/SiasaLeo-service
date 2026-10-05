package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment_asset; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment_asset")
@IdClass(CommitmentAttachmentId.class)
public class CommitmentAttachment {
    @Id
    @Column(name = "commitment_revision_id", nullable = false)
    private Long commitmentRevisionId;

    @Id
    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Column(name = "caption", nullable = true)
    private String caption;

    @Column(name = "locator", nullable = true, length = 255)
    private String locator;

    public CommitmentAttachment() {}

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getCaption() { return caption; }
    public void setCaption(String value) { caption = value; }

    public String getLocator() { return locator; }
    public void setLocator(String value) { locator = value; }
}
