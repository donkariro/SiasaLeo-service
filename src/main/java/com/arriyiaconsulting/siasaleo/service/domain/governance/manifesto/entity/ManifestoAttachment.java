package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for manifesto_asset; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "manifesto_asset")
@IdClass(ManifestoAttachmentId.class)
public class ManifestoAttachment {
    @Id
    @Column(name = "manifesto_version_id", nullable = false)
    private Long manifestoVersionId;

    @Id
    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Column(name = "caption", nullable = true)
    private String caption;

    @Column(name = "locator", nullable = true, length = 255)
    private String locator;

    public ManifestoAttachment() {}

    public Long getManifestoVersionId() { return manifestoVersionId; }
    public void setManifestoVersionId(Long value) { manifestoVersionId = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getCaption() { return caption; }
    public void setCaption(String value) { caption = value; }

    public String getLocator() { return locator; }
    public void setLocator(String value) { locator = value; }
}
