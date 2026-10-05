package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ManifestoAttachmentRepository extends BasicRepository<ManifestoAttachment, ManifestoAttachmentId> {
    @Insert ManifestoAttachment insert(ManifestoAttachment entity);
    @Update ManifestoAttachment update(ManifestoAttachment entity);
    @Query("FROM ManifestoAttachment ORDER BY manifestoVersionId, assetVersionId")
    List<ManifestoAttachment> ordered();
    @Query("FROM ManifestoAttachment WHERE manifestoVersionId = :id ORDER BY assetVersionId")
    List<ManifestoAttachment> forVersion(@Param("id") Long id);
}
