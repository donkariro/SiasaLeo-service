package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface TargetObservationRepository extends BasicRepository<TargetObservation, TargetObservationId> {
    @Insert TargetObservation insert(TargetObservation entity);
    @Update TargetObservation update(TargetObservation entity);
    @Query("FROM TargetObservation ORDER BY targetId, observationId")
    List<TargetObservation> ordered();
}
