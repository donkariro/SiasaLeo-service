package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ClaimRevisionRepository extends BasicRepository<ClaimRevision, Long> {
    @Insert ClaimRevision insert(ClaimRevision entity);
    @Update ClaimRevision update(ClaimRevision entity);
    @Query("FROM ClaimRevision ORDER BY id")
    List<ClaimRevision> ordered();
    @Query("SELECT MAX(revisionNumber) FROM ClaimRevision WHERE claimId = :id")
    Integer latestNumber(@Param("id") Long id);

    @Query("FROM ClaimRevision r WHERE r.officeholderId IN (SELECT o.id FROM Officeholder o WHERE o.personId = :person) AND r.publicationStatus = :status ORDER BY r.officeholderId, r.claimId, r.revisionNumber")
    List<ClaimRevision> forPerson(@Param("person") Long person, @Param("status") com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus status, PageRequest page);

    @Query("SELECT publishedAt FROM ClaimRevision WHERE id = :id")
    java.time.OffsetDateTime publicationTime(@Param("id") Long id);
}
