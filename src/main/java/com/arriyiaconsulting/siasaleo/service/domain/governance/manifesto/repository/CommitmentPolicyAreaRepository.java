package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentPolicyAreaRepository extends BasicRepository<CommitmentPolicyArea, CommitmentPolicyAreaId> {
    @Insert CommitmentPolicyArea insert(CommitmentPolicyArea entity);
    @Update CommitmentPolicyArea update(CommitmentPolicyArea entity);
    @Query("FROM CommitmentPolicyArea ORDER BY commitmentRevisionId, policyAreaId")
    List<CommitmentPolicyArea> ordered();
}
