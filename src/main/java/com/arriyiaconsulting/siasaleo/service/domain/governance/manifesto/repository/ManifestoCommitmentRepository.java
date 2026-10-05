package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ManifestoCommitmentRepository extends BasicRepository<ManifestoCommitment, ManifestoCommitmentId> {
    @Insert ManifestoCommitment insert(ManifestoCommitment entity);
    @Update ManifestoCommitment update(ManifestoCommitment entity);
    @Query("FROM ManifestoCommitment ORDER BY manifestoVersionId, commitmentRevisionId")
    List<ManifestoCommitment> ordered();
    @Query("FROM ManifestoCommitment WHERE manifestoVersionId = :id ORDER BY commitmentRevisionId")
    List<ManifestoCommitment> forVersion(@Param("id") Long id);
}
