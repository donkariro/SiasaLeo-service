package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ObservationRepository extends BasicRepository<Observation, Long> {
    @Insert Observation insert(Observation entity);
    @Update Observation update(Observation entity);
    @Query("FROM Observation ORDER BY id")
    List<Observation> ordered();
    @Query("SELECT publishedAt FROM Observation WHERE id = :id")
    java.time.OffsetDateTime publicationTime(@Param("id") Long id);
}
