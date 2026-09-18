package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping.GeographySnapshotMapper;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.GeographySnapshotRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
@Transactional
public class GeographySnapshotService {
    @Inject private GeographySnapshotRepository snapshots;
    @Inject private GeographySnapshotMapper mapper;

    public List<GeographySnapshotDto> list(boolean administrator, int page, int size) {
        return snapshots.list(administrator, offset(page, size), limit(size)).stream().map(mapper::toDto).toList();
    }

    public GeographySnapshotDto find(Long id, boolean administrator) {
        return mapper.toDto(requireVisible(id, administrator));
    }

    public ElectoralGeographySnapshot requireVisible(Long id, boolean administrator) {
        return snapshots.find(id).filter(s -> administrator || s.getStatus() == SnapshotStatus.PUBLISHED)
                .orElseThrow(GeographyNotFoundException::new);
    }

    public ElectoralGeographySnapshot lockDraft(Long id) {
        ElectoralGeographySnapshot snapshot = snapshots.lock(id).orElseThrow(GeographyNotFoundException::new);
        snapshot.requireDraft();
        return snapshot;
    }

    public GeographySnapshotDto create(CreateGeographySnapshotRequest request) {
        if (request == null) throw new IllegalArgumentException("Snapshot metadata is required");
        return mapper.toDto(snapshots.insert(mapper.toEntity(request, null)));
    }

    public GeographySnapshotDto reviseMetadata(Long id, CreateGeographySnapshotRequest request) {
        if (request == null) throw new IllegalArgumentException("Snapshot metadata is required");
        ElectoralGeographySnapshot snapshot = lockDraft(id);
        snapshot.revise(request.name(), request.referenceDate(), request.sourceReference(), request.sourceSha256());
        return mapper.toDto(snapshot);
    }

    public GeographySnapshotDto createRevision(Long sourceId, CreateGeographySnapshotRequest request) {
        if (request == null) throw new IllegalArgumentException("Revision metadata is required");
        ElectoralGeographySnapshot original = requireVisible(sourceId, true);
        if (original.getStatus() != SnapshotStatus.PUBLISHED) {
            throw new IllegalArgumentException("Revisions must start from a published snapshot");
        }
        ElectoralGeographySnapshot revision = snapshots.insert(mapper.toEntity(request, sourceId));
        snapshots.copyAreas(sourceId, revision.getId());
        return mapper.toDto(revision);
    }

    static int limit(int size) { return Math.max(1, Math.min(size, 500)); }
    static int offset(int page, int size) {
        if (page < 0 || (long) page * limit(size) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Page is out of range");
        }
        return page * limit(size);
    }
}
