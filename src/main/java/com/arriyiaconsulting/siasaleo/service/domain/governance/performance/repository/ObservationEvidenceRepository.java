package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ObservationEvidenceRepository extends BasicRepository<ObservationEvidence, ObservationEvidenceId> {
    @Insert ObservationEvidence insert(ObservationEvidence entity);
    @Update ObservationEvidence update(ObservationEvidence entity);
    @Query("FROM ObservationEvidence ORDER BY observationId, assetVersionId, relationshipCode")
    List<ObservationEvidence> ordered();
    @Query("FROM ObservationEvidence WHERE observationId = :id ORDER BY assetVersionId, relationshipCode")
    List<ObservationEvidence> forObservation(@Param("id") Long id);
}
