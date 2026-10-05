package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for digital_asset_version; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "digital_asset_version")
public class DigitalAssetVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "source_url", nullable = true, length = 2048)
    private String sourceUrl;

    @Column(name = "author_party_id", nullable = true)
    private Long authorPartyId;

    @Column(name = "publisher_party_id", nullable = true)
    private Long publisherPartyId;

    @Column(name = "source_published_on", nullable = true)
    private LocalDate sourcePublishedOn;

    @Column(name = "provenance", nullable = false)
    private String provenance;

    @Column(name = "recorded_by_account_id", nullable = true)
    private Long recordedByAccountId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "sealed_at", nullable = true)
    private OffsetDateTime sealedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public DigitalAssetVersion() {}

    public Long getId() { return id; }

    public Long getAssetId() { return assetId; }
    public void setAssetId(Long value) { assetId = value; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer value) { versionNumber = value; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String value) { sourceUrl = value; }

    public Long getAuthorPartyId() { return authorPartyId; }
    public void setAuthorPartyId(Long value) { authorPartyId = value; }

    public Long getPublisherPartyId() { return publisherPartyId; }
    public void setPublisherPartyId(Long value) { publisherPartyId = value; }

    public LocalDate getSourcePublishedOn() { return sourcePublishedOn; }
    public void setSourcePublishedOn(LocalDate value) { sourcePublishedOn = value; }

    public String getProvenance() { return provenance; }
    public void setProvenance(String value) { provenance = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime value) { recordedAt = value; }

    public OffsetDateTime getSealedAt() { return sealedAt; }
    public void setSealedAt(OffsetDateTime value) { sealedAt = value; }

    public Long getRowVersion() { return rowVersion; }
}
