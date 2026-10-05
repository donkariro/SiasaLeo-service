package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class ObservationEvidenceId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long observationId;
    public Long assetVersionId;
    public String relationshipCode;
    public ObservationEvidenceId() {}
    public ObservationEvidenceId(Long observationId, Long assetVersionId, String relationshipCode) {
        this.observationId = observationId;
        this.assetVersionId = assetVersionId;
        this.relationshipCode = relationshipCode;
    }
    @Override public boolean equals(Object other) {
        return other instanceof ObservationEvidenceId that && Objects.equals(observationId, that.observationId) && Objects.equals(assetVersionId, that.assetVersionId) && Objects.equals(relationshipCode, that.relationshipCode);
    }
    @Override public int hashCode() { return Objects.hash(observationId, assetVersionId, relationshipCode); }
}
