package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentRevisionRepository extends BasicRepository<CommitmentRevision, Long> {
    @Insert CommitmentRevision insert(CommitmentRevision entity);
    @Update CommitmentRevision update(CommitmentRevision entity);
    @Query("FROM CommitmentRevision ORDER BY id")
    List<CommitmentRevision> ordered();
    @Query("SELECT MAX(revisionNumber) FROM CommitmentRevision WHERE commitmentId = :id")
    Integer latestNumber(@Param("id") Long id);

    @Query("SELECT COUNT(v.id) FROM ManifestoVersion v, ManifestoCommitment mc WHERE mc.manifestoVersionId = v.id AND mc.commitmentRevisionId = :id AND v.publicationStatus <> :draft")
    long publicVersionCount(@Param("id") Long id, @Param("draft") com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus draft);
}
