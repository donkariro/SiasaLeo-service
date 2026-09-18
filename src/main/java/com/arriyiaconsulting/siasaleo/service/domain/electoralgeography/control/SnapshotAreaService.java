package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping.GeographySnapshotMapper;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import static com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control.GeographySnapshotService.*;

@ApplicationScoped
@Transactional
public class SnapshotAreaService {
    @Inject private GeographySnapshotService snapshots;
    @Inject private GeographySnapshotRepository snapshotRepository;
    @Inject private ElectoralAreaSnapshotRepository areas;
    @Inject private ElectoralAreaRepository operationalAreas;
    @Inject private AreaTypeRepository types;
    @Inject private GeographySnapshotMapper mapper;

    public ElectoralAreaSnapshotDto find(Long snapshotId, Long areaId, boolean administrator) {
        snapshots.requireVisible(snapshotId, administrator);
        return mapper.toDto(requireArea(snapshotId, areaId));
    }

    public List<ElectoralAreaSnapshotDto> byType(Long snapshotId, String type, boolean administrator, int page, int size) {
        snapshots.requireVisible(snapshotId, administrator);
        requireType(type);
        return areas.byType(snapshotId, type, offset(page, size), limit(size)).stream().map(mapper::toDto).toList();
    }

    public List<ElectoralAreaSnapshotDto> children(Long snapshotId, Long areaId, boolean administrator, int page, int size) {
        snapshots.requireVisible(snapshotId, administrator);
        requireArea(snapshotId, areaId);
        return areas.children(snapshotId, areaId, offset(page, size), limit(size)).stream().map(mapper::toDto).toList();
    }

    public List<ElectoralAreaSnapshotDto> descendants(Long snapshotId, Long areaId, boolean administrator, int page, int size) {
        snapshots.requireVisible(snapshotId, administrator);
        ElectoralAreaSnapshot area = requireArea(snapshotId, areaId);
        String prefix = area.getAncestorPath() + area.getId() + "/%";
        return areas.descendants(snapshotId, prefix, offset(page, size), limit(size)).stream().map(mapper::toDto).toList();
    }

    public ElectoralAreaSnapshotDto create(Long snapshotId, CreateSnapshotAreaRequest request) {
        if (request == null) throw new IllegalArgumentException("Area details are required");
        snapshots.lockDraft(snapshotId);
        AreaType type = requireType(request.areaType());
        ElectoralAreaSnapshot parent = request.parentId() == null ? null : requireArea(snapshotId, request.parentId());
        String path = SnapshotHierarchy.path(snapshotId, type.getName(), parent);
        if (parent == null && areas.hasRoot(snapshotId)) {
            throw new IllegalArgumentException("Snapshot already has a root");
        }
        checkCode(snapshotId, request.parentId(), type.getId(), request.areaCode(), null);
        return mapper.toDto(areas.insert(mapper.toEntity(request, snapshotId, type, path)));
    }

    public ElectoralAreaSnapshotDto update(Long snapshotId, Long areaId, UpdateSnapshotAreaRequest request) {
        if (request == null) throw new IllegalArgumentException("Area details are required");
        snapshots.lockDraft(snapshotId);
        ElectoralAreaSnapshot area = requireArea(snapshotId, areaId);
        checkCode(snapshotId, area.getParentId(), area.getAreaType().getId(), request.areaCode(), areaId);
        area.revise(request.name(), request.areaCode(), request.sourceRecordReference());
        return mapper.toDto(area);
    }

    public void delete(Long snapshotId, Long areaId) {
        snapshots.lockDraft(snapshotId);
        ElectoralAreaSnapshot area = requireArea(snapshotId, areaId);
        if (!areas.children(snapshotId, areaId, 0, 1).isEmpty()) {
            throw new IllegalArgumentException("Remove children before deleting a draft area");
        }
        areas.deleteCorrespondence(areaId);
        areas.delete(area);
    }

    public AreaCorrespondenceDto correspondence(Long snapshotId, Long areaId) {
        snapshots.requireVisible(snapshotId, true);
        requireArea(snapshotId, areaId);
        return areas.correspondence(areaId).map(mapper::toDto).orElseThrow(GeographyNotFoundException::new);
    }

    public AreaCorrespondenceDto reviewCorrespondence(Long snapshotId, Long areaId, ReviewAreaCorrespondenceRequest request) {
        if (request == null || request.electoralAreaId() == null || request.evidenceReference() == null
                || request.evidenceReference().isBlank() || request.evidenceReference().length() > 2048) {
            throw new IllegalArgumentException("Operational area and evidence reference are required");
        }
        // Serialize correspondence changes too; these do not modify the snapshot itself.
        snapshotRepository.lock(snapshotId).orElseThrow(GeographyNotFoundException::new);
        ElectoralAreaSnapshot area = requireArea(snapshotId, areaId);
        ElectoralArea operational = operationalAreas.findById(request.electoralAreaId()).orElseThrow(GeographyNotFoundException::new);
        if (!area.getAreaType().getId().equals(operational.getAreaType().getId())) {
            throw new IllegalArgumentException("Corresponding areas must have the same type");
        }
        ElectoralAreaCorrespondence match = areas.correspondence(areaId).orElse(null);
        if (match == null) {
            match = areas.insert(new ElectoralAreaCorrespondence(request.electoralAreaId(), areaId,
                    request.evidenceReference(), OffsetDateTime.now()));
        } else {
            match.review(request.electoralAreaId(), request.evidenceReference());
        }
        return mapper.toDto(match);
    }

    private ElectoralAreaSnapshot requireArea(Long snapshotId, Long areaId) {
        return areas.find(snapshotId, areaId).orElseThrow(GeographyNotFoundException::new);
    }

    private AreaType requireType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Area type is required");
        return types.findByName(type).orElseThrow(() -> new IllegalArgumentException("Unknown area type: " + type));
    }

    private void checkCode(Long snapshotId, Long parentId, Long typeId, String code, Long exceptId) {
        if (areas.duplicateCode(snapshotId, parentId, typeId, code, exceptId)) {
            throw new IllegalArgumentException("Area code already exists for this parent and type");
        }
    }
}
