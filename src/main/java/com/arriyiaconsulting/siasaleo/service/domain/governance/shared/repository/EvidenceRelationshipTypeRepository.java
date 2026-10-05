package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface EvidenceRelationshipTypeRepository extends BasicRepository<EvidenceRelationshipType, String> {
    @Insert EvidenceRelationshipType insert(EvidenceRelationshipType entity);
    @Update EvidenceRelationshipType update(EvidenceRelationshipType entity);
    @Query("FROM EvidenceRelationshipType ORDER BY code")
    List<EvidenceRelationshipType> ordered();
}
