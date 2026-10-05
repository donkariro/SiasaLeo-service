package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import java.io.Serializable;
import java.util.Objects;

public class CommitmentPolicyAreaId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long commitmentRevisionId;
    public Long policyAreaId;
    public CommitmentPolicyAreaId() {}
    public CommitmentPolicyAreaId(Long commitmentRevisionId, Long policyAreaId) {
        this.commitmentRevisionId = commitmentRevisionId;
        this.policyAreaId = policyAreaId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof CommitmentPolicyAreaId that && Objects.equals(commitmentRevisionId, that.commitmentRevisionId) && Objects.equals(policyAreaId, that.policyAreaId);
    }
    @Override public int hashCode() { return Objects.hash(commitmentRevisionId, policyAreaId); }
}
