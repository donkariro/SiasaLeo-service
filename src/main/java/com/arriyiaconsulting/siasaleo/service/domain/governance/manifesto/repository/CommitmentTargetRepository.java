package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentTargetRepository extends BasicRepository<CommitmentTarget, Long> {
    @Insert CommitmentTarget insert(CommitmentTarget entity);
    @Update CommitmentTarget update(CommitmentTarget entity);
    @Query("FROM CommitmentTarget ORDER BY id")
    List<CommitmentTarget> ordered();
    @Query("FROM CommitmentTarget WHERE commitmentRevisionId = :id ORDER BY id")
    List<CommitmentTarget> forRevision(@Param("id") Long id);
}
