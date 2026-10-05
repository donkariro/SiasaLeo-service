package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class WorkEffortAssetId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long workEffortId;
    public Long assetVersionId;
    public WorkEffortAssetId() {}
    public WorkEffortAssetId(Long workEffortId, Long assetVersionId) {
        this.workEffortId = workEffortId;
        this.assetVersionId = assetVersionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof WorkEffortAssetId that && Objects.equals(workEffortId, that.workEffortId) && Objects.equals(assetVersionId, that.assetVersionId);
    }
    @Override public int hashCode() { return Objects.hash(workEffortId, assetVersionId); }
}
