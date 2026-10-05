package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentGoalRepository extends BasicRepository<CommitmentGoal, CommitmentGoalId> {
    @Insert CommitmentGoal insert(CommitmentGoal entity);
    @Update CommitmentGoal update(CommitmentGoal entity);
    @Query("FROM CommitmentGoal ORDER BY commitmentRevisionId, goalId")
    List<CommitmentGoal> ordered();
}
