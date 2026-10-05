package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class ClaimEvidenceId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long recordId;
    public Long assetVersionId;
    public String relationshipCode;
    public ClaimEvidenceId() {}
    public ClaimEvidenceId(Long recordId, Long assetVersionId, String relationshipCode) {
        this.recordId = recordId;
        this.assetVersionId = assetVersionId;
        this.relationshipCode = relationshipCode;
    }
    @Override public boolean equals(Object other) {
        return other instanceof ClaimEvidenceId that && Objects.equals(recordId, that.recordId) && Objects.equals(assetVersionId, that.assetVersionId) && Objects.equals(relationshipCode, that.relationshipCode);
    }
    @Override public int hashCode() { return Objects.hash(recordId, assetVersionId, relationshipCode); }
}
