package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ElectoralAreaSnapshotRepository {
    @PersistenceContext private EntityManager em;

    public Optional<ElectoralAreaSnapshot> find(Long snapshotId, Long id) {
        return em.createQuery("SELECT a FROM ElectoralAreaSnapshot a WHERE a.snapshotId = :snapshot AND a.id = :id",
                ElectoralAreaSnapshot.class).setParameter("snapshot", snapshotId).setParameter("id", id)
                .getResultStream().findFirst();
    }

    public List<ElectoralAreaSnapshot> all(Long snapshotId) {
        return em.createQuery("SELECT a FROM ElectoralAreaSnapshot a JOIN FETCH a.areaType WHERE a.snapshotId = :snapshot ORDER BY a.id",
                ElectoralAreaSnapshot.class).setParameter("snapshot", snapshotId).getResultList();
    }

    public List<ElectoralAreaSnapshot> byType(Long snapshotId, String type, int offset, int size) {
        return em.createQuery("SELECT a FROM ElectoralAreaSnapshot a WHERE a.snapshotId = :snapshot AND a.areaType.name = :type ORDER BY a.id",
                ElectoralAreaSnapshot.class).setParameter("snapshot", snapshotId).setParameter("type", type)
                .setFirstResult(offset).setMaxResults(size).getResultList();
    }

    public List<ElectoralAreaSnapshot> children(Long snapshotId, Long parentId, int offset, int size) {
        return em.createQuery("SELECT a FROM ElectoralAreaSnapshot a WHERE a.snapshotId = :snapshot AND a.parentId = :parent ORDER BY a.id",
                ElectoralAreaSnapshot.class).setParameter("snapshot", snapshotId).setParameter("parent", parentId)
                .setFirstResult(offset).setMaxResults(size).getResultList();
    }

    public List<ElectoralAreaSnapshot> descendants(Long snapshotId, String prefix, int offset, int size) {
        return em.createQuery("SELECT a FROM ElectoralAreaSnapshot a WHERE a.snapshotId = :snapshot AND a.ancestorPath LIKE :prefix ORDER BY a.id",
                ElectoralAreaSnapshot.class).setParameter("snapshot", snapshotId).setParameter("prefix", prefix)
                .setFirstResult(offset).setMaxResults(size).getResultList();
    }

    public boolean duplicateCode(Long snapshotId, Long parentId, Long typeId, String code, Long exceptId) {
        if (code == null) return false;
        return em.createQuery("SELECT COUNT(a) FROM ElectoralAreaSnapshot a WHERE a.snapshotId = :snapshot "
                + "AND (a.parentId = :parent OR (a.parentId IS NULL AND :parent IS NULL)) "
                + "AND a.areaType.id = :type AND a.areaCode = :code AND (:except IS NULL OR a.id <> :except)", Long.class)
                .setParameter("snapshot", snapshotId).setParameter("parent", parentId).setParameter("type", typeId)
                .setParameter("code", code).setParameter("except", exceptId).getSingleResult() > 0;
    }

    public boolean hasRoot(Long snapshotId) {
        return em.createQuery("SELECT COUNT(a) FROM ElectoralAreaSnapshot a WHERE a.snapshotId = :snapshot AND a.parentId IS NULL", Long.class)
                .setParameter("snapshot", snapshotId).getSingleResult() > 0;
    }

    public ElectoralAreaSnapshot insert(ElectoralAreaSnapshot area) {
        em.persist(area);
        em.flush();
        return area;
    }

    public void delete(ElectoralAreaSnapshot area) { em.remove(area); }

    public Optional<ElectoralAreaCorrespondence> correspondence(Long areaId) {
        return em.createQuery("SELECT c FROM ElectoralAreaCorrespondence c WHERE c.areaSnapshotId = :area", ElectoralAreaCorrespondence.class)
                .setParameter("area", areaId).getResultStream().findFirst();
    }

    public ElectoralAreaCorrespondence insert(ElectoralAreaCorrespondence correspondence) {
        em.persist(correspondence);
        em.flush();
        return correspondence;
    }

    public void deleteCorrespondence(Long areaId) {
        correspondence(areaId).ifPresent(em::remove);
        em.flush();
    }
}
