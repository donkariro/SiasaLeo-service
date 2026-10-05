package com.arriyiaconsulting.siasaleo.service.domain.office.repository;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface OfficeholderEvidenceRepository extends BasicRepository<OfficeholderEvidence, OfficeholderEvidenceId> {
    @Insert OfficeholderEvidence insert(OfficeholderEvidence entity);
    @Update OfficeholderEvidence update(OfficeholderEvidence entity);
    @Query("FROM OfficeholderEvidence ORDER BY officeholderId, assetVersionId")
    List<OfficeholderEvidence> ordered();
}
