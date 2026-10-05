package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for work_effort_asset; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "work_effort_asset")
@IdClass(WorkEffortAssetId.class)
public class WorkEffortAsset {
    @Id
    @Column(name = "work_effort_id", nullable = false)
    private Long workEffortId;

    @Id
    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Column(name = "caption", nullable = true)
    private String caption;

    @Column(name = "locator", nullable = true, length = 255)
    private String locator;

    public WorkEffortAsset() {}

    public Long getWorkEffortId() { return workEffortId; }
    public void setWorkEffortId(Long value) { workEffortId = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getCaption() { return caption; }
    public void setCaption(String value) { caption = value; }

    public String getLocator() { return locator; }
    public void setLocator(String value) { locator = value; }
}
