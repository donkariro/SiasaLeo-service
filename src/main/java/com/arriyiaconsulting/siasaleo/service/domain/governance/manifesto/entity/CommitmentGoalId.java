package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import java.io.Serializable;
import java.util.Objects;

public class CommitmentGoalId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long commitmentRevisionId;
    public Long goalId;
    public CommitmentGoalId() {}
    public CommitmentGoalId(Long commitmentRevisionId, Long goalId) {
        this.commitmentRevisionId = commitmentRevisionId;
        this.goalId = goalId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof CommitmentGoalId that && Objects.equals(commitmentRevisionId, that.commitmentRevisionId) && Objects.equals(goalId, that.goalId);
    }
    @Override public int hashCode() { return Objects.hash(commitmentRevisionId, goalId); }
}
