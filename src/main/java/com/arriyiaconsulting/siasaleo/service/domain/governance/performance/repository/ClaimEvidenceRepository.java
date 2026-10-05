package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ClaimEvidenceRepository extends BasicRepository<ClaimEvidence, ClaimEvidenceId> {
    @Insert ClaimEvidence insert(ClaimEvidence entity);
    @Update ClaimEvidence update(ClaimEvidence entity);
    @Query("FROM ClaimEvidence ORDER BY recordId, assetVersionId, relationshipCode")
    List<ClaimEvidence> ordered();
    @Query("FROM ClaimEvidence WHERE recordId = :id ORDER BY assetVersionId, relationshipCode")
    List<ClaimEvidence> forRecord(@Param("id") Long id);
}
