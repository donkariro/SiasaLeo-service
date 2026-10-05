package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ManifestoVersionRepository extends BasicRepository<ManifestoVersion, Long> {
    @Insert ManifestoVersion insert(ManifestoVersion entity);
    @Update ManifestoVersion update(ManifestoVersion entity);
    @Query("FROM ManifestoVersion ORDER BY id")
    List<ManifestoVersion> ordered();
    @Query("SELECT MAX(versionNumber) FROM ManifestoVersion WHERE manifestoId = :id")
    Integer latestNumber(@Param("id") Long id);

    @Query("FROM ManifestoVersion v WHERE v.manifestoId IN (SELECT m.id FROM Manifesto m WHERE m.issuerPersonId = :person) AND v.publicationStatus = :status ORDER BY v.publishedAt DESC, v.id DESC")
    List<ManifestoVersion> forPerson(@Param("person") Long person, @Param("status") com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus status, PageRequest page);

    @Query("SELECT publishedAt FROM ManifestoVersion WHERE id = :id")
    java.time.OffsetDateTime publicationTime(@Param("id") Long id);
}
