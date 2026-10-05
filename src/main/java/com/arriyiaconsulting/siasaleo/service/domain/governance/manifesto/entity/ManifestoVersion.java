package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for manifesto_version; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "manifesto_version")
public class ManifestoVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "manifesto_id", nullable = false)
    private Long manifestoId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "language", nullable = false, length = 35)
    private String language = "en";

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 15)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(name = "published_at", nullable = true, insertable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "recorded_by_account_id", nullable = false)
    private Long recordedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    public ManifestoVersion() {}

    public Long getId() { return id; }

    public Long getManifestoId() { return manifestoId; }
    public void setManifestoId(Long value) { manifestoId = value; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer value) { versionNumber = value; }

    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }

    public String getLanguage() { return language; }
    public void setLanguage(String value) { language = value; }

    public String getContent() { return content; }
    public void setContent(String value) { content = value; }

    public PublicationStatus getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(PublicationStatus value) { publicationStatus = value; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }

    public Long getRecordedByAccountId() { return recordedByAccountId; }
    public void setRecordedByAccountId(Long value) { recordedByAccountId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }

    public Long getRowVersion() { return rowVersion; }
}
