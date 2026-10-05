package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface AssessmentEvidenceRepository extends BasicRepository<AssessmentEvidence, AssessmentEvidenceId> {
    @Insert AssessmentEvidence insert(AssessmentEvidence entity);
    @Update AssessmentEvidence update(AssessmentEvidence entity);
    @Query("FROM AssessmentEvidence ORDER BY assessmentId, assetVersionId, relationshipCode")
    List<AssessmentEvidence> ordered();
    @Query("FROM AssessmentEvidence WHERE assessmentId = :id ORDER BY assetVersionId, relationshipCode")
    List<AssessmentEvidence> forAssessment(@Param("id") Long id);
}
