package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentAttachmentRepository extends BasicRepository<CommitmentAttachment, CommitmentAttachmentId> {
    @Insert CommitmentAttachment insert(CommitmentAttachment entity);
    @Update CommitmentAttachment update(CommitmentAttachment entity);
    @Query("FROM CommitmentAttachment ORDER BY commitmentRevisionId, assetVersionId")
    List<CommitmentAttachment> ordered();
}
