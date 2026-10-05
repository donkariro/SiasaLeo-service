package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class WorkEffortPolicyAreaId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long workEffortId;
    public Long policyAreaId;
    public WorkEffortPolicyAreaId() {}
    public WorkEffortPolicyAreaId(Long workEffortId, Long policyAreaId) {
        this.workEffortId = workEffortId;
        this.policyAreaId = policyAreaId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof WorkEffortPolicyAreaId that && Objects.equals(workEffortId, that.workEffortId) && Objects.equals(policyAreaId, that.policyAreaId);
    }
    @Override public int hashCode() { return Objects.hash(workEffortId, policyAreaId); }
}
