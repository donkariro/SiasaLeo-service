package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment_goal; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment_goal")
@IdClass(CommitmentGoalId.class)
public class CommitmentGoal {
    @Id
    @Column(name = "commitment_revision_id", nullable = false)
    private Long commitmentRevisionId;

    @Id
    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    public CommitmentGoal() {}

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long value) { goalId = value; }
}
