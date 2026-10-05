package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface WorkEffortParticipationRepository extends BasicRepository<WorkEffortParticipation, Long> {
    @Insert WorkEffortParticipation insert(WorkEffortParticipation entity);
    @Update WorkEffortParticipation update(WorkEffortParticipation entity);
    @Query("FROM WorkEffortParticipation ORDER BY id")
    List<WorkEffortParticipation> ordered();
    @Query("FROM WorkEffortParticipation WHERE workEffortId = :id ORDER BY id")
    List<WorkEffortParticipation> forWork(@Param("id") Long id);

    @Query("FROM WorkEffortParticipation p WHERE p.partyId = :person AND (EXISTS (SELECT r.id FROM ClaimRevision r WHERE r.workEffortId = p.workEffortId AND r.publicationStatus = :status) OR EXISTS (SELECT v.id FROM CommitmentDeliveryLink d, ManifestoCommitment mc, ManifestoVersion v WHERE d.workEffortId = p.workEffortId AND mc.commitmentRevisionId = d.commitmentRevisionId AND v.id = mc.manifestoVersionId AND v.publicationStatus = :status)) ORDER BY p.id")
    List<WorkEffortParticipation> publicContributions(@Param("person") Long person, @Param("status") com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus status, PageRequest page);
}
