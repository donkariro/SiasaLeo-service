package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for digital_asset; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "digital_asset")
public class DigitalAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "asset_type", nullable = false, length = 40)
    private String assetType;

    @Column(name = "created_by_account_id", nullable = true)
    private Long createdByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public DigitalAsset() {}

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String value) { assetType = value; }

    public Long getCreatedByAccountId() { return createdByAccountId; }
    public void setCreatedByAccountId(Long value) { createdByAccountId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
