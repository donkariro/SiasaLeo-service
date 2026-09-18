package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

/** JPA is used here for explicit publication/write locking and draft copying. */
@ApplicationScoped
public class GeographySnapshotRepository {
    @PersistenceContext private EntityManager em;

    public Optional<ElectoralGeographySnapshot> find(Long id) {
        return Optional.ofNullable(em.find(ElectoralGeographySnapshot.class, id));
    }

    public Optional<ElectoralGeographySnapshot> lock(Long id) {
        return Optional.ofNullable(em.find(ElectoralGeographySnapshot.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<ElectoralGeographySnapshot> list(boolean includeDrafts, int offset, int size) {
        return em.createQuery("SELECT s FROM ElectoralGeographySnapshot s "
                + "WHERE :drafts = true OR s.status = :published ORDER BY s.id DESC", ElectoralGeographySnapshot.class)
                .setParameter("drafts", includeDrafts).setParameter("published", SnapshotStatus.PUBLISHED)
                .setFirstResult(offset).setMaxResults(size).getResultList();
    }

    public ElectoralGeographySnapshot insert(ElectoralGeographySnapshot snapshot) {
        em.persist(snapshot);
        em.flush();
        return snapshot;
    }

    public void copyAreas(Long sourceId, Long targetId) {
        em.createNativeQuery("SELECT copy_geography_snapshot(:source, :target)")
                .setParameter("source", sourceId).setParameter("target", targetId).getSingleResult();
    }

    public void flush() { em.flush(); }
}
