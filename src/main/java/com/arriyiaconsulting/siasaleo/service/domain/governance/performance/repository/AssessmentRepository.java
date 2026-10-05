package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface AssessmentRepository extends BasicRepository<Assessment, Long> {
    @Insert Assessment insert(Assessment entity);
    @Update Assessment update(Assessment entity);
    @Query("FROM Assessment ORDER BY id")
    List<Assessment> ordered();
    @Query("FROM Assessment WHERE recordId = :id AND publicationStatus = :status ORDER BY assessedAt, id")
    List<Assessment> forRecord(@Param("id") Long id, @Param("status") com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus status);

    @Query("FROM Assessment WHERE commitmentRevisionId = :id AND publicationStatus = :status ORDER BY assessedAt, id")
    List<Assessment> forRevision(@Param("id") Long id, @Param("status") com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus status);

    @Query("SELECT publishedAt FROM Assessment WHERE id = :id")
    java.time.OffsetDateTime publicationTime(@Param("id") Long id);
}
