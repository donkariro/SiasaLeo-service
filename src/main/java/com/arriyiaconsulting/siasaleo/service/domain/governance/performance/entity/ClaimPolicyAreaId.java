package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class ClaimPolicyAreaId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long recordId;
    public Long policyAreaId;
    public ClaimPolicyAreaId() {}
    public ClaimPolicyAreaId(Long recordId, Long policyAreaId) {
        this.recordId = recordId;
        this.policyAreaId = policyAreaId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof ClaimPolicyAreaId that && Objects.equals(recordId, that.recordId) && Objects.equals(policyAreaId, that.policyAreaId);
    }
    @Override public int hashCode() { return Objects.hash(recordId, policyAreaId); }
}
