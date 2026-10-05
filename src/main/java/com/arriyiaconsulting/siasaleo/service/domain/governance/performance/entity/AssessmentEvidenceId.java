package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class AssessmentEvidenceId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long assessmentId;
    public Long assetVersionId;
    public String relationshipCode;
    public AssessmentEvidenceId() {}
    public AssessmentEvidenceId(Long assessmentId, Long assetVersionId, String relationshipCode) {
        this.assessmentId = assessmentId;
        this.assetVersionId = assetVersionId;
        this.relationshipCode = relationshipCode;
    }
    @Override public boolean equals(Object other) {
        return other instanceof AssessmentEvidenceId that && Objects.equals(assessmentId, that.assessmentId) && Objects.equals(assetVersionId, that.assetVersionId) && Objects.equals(relationshipCode, that.relationshipCode);
    }
    @Override public int hashCode() { return Objects.hash(assessmentId, assetVersionId, relationshipCode); }
}
