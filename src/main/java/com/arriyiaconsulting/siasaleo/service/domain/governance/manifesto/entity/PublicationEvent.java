package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for publication_event; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "publication_event")
public class PublicationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "manifesto_version_id", nullable = false)
    private Long manifestoVersionId;

    @Column(name = "announced_at", nullable = false)
    private OffsetDateTime announcedAt;

    @Column(name = "venue", nullable = false)
    private String venue;

    @Column(name = "asset_version_id", nullable = true)
    private Long assetVersionId;

    @Column(name = "recorded_by_account_id", nullable = false)
    private Long recordedByAccountId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public PublicationEvent() {}

    public Long getId() { return id; }

    public Long getManifestoVersionId() { return manifestoVersionId; }
    public void setManifestoVersionId(Long value) { manifestoVersionId = value; }

    public OffsetDateTime getAnnouncedAt() { return announcedAt; }
    public void setAnnouncedAt(OffsetDateTime value) { announcedAt = value; }

    public String getVenue() { return venue; }
    public void setVenue(String value) { venue = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime value) { recordedAt = value; }
}
