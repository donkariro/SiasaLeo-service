package com.arriyiaconsulting.siasaleo.service.domain.election.control;

import com.arriyiaconsulting.siasaleo.service.domain.election.mapping.ElectionMapper;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.*;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ElectionService {

    @Inject
    private ElectionMapper electionMapper;
    @Inject private ElectionCycleRepository cycles;
    @Inject private ElectionTypeRepository types;
    @Inject private ElectionStatusRepository statuses;
    @Inject private ElectionEventRepository events;
    @Inject private com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.GeographySnapshotRepository geography;

    public List<ElectionCycleDto> cycles() {
        return cycles.findAllOrdered().stream().map(electionMapper::toElectionCycleDto).toList();
    }

    public Optional<ElectionCycleDto> findCycle(Long id) {
        return cycles.findById(id).map(electionMapper::toElectionCycleDto);
    }

    public List<ElectionTypeDto> types() {
        return types.findAllOrdered().stream().map(electionMapper::toElectionTypeDto).toList();
    }

    public List<ElectionStatusDto> statuses() {
        return statuses.findAllOrdered().stream().map(electionMapper::toElectionStatusDto).toList();
    }

    public Optional<ElectionEventDto> findEvent(Long id) {
        return events.findById(id).map(electionMapper::toElectionEventDto);
    }

    public List<ElectionEventDto> events(Long cycleId, Long typeId, Long statusId, int page, int size) {
        PageRequest paging = pageRequest(page, size);
        if (cycleId != null) requireCycle(cycleId);
        if (typeId != null) requireType(typeId);
        if (statusId != null) requireStatus(statusId);
        return events.search(cycleId, typeId, statusId, paging).stream().map(electionMapper::toElectionEventDto).toList();
    }

    @Transactional
    public ElectionEventDto createEvent(CreateElectionEventRequest request) {
        if (request == null || request.electionCycleId() == null
                || request.electionDate() == null || request.typeId() == null) {
            throw new IllegalArgumentException("Election cycle, date and type are required");
        }
        ElectionCycle cycle = requireCycle(request.electionCycleId());
        int year = request.electionDate().getYear();
        if (year < cycle.getFromYear() || year >= cycle.getUptoYear()) {
            throw new IllegalArgumentException("Election date must fall within the cycle's years "
                    + "(fromYear inclusive, uptoYear exclusive)");
        }
        ElectionType type = requireType(request.typeId());
        ElectionStatus initial = request.statusId() == null
                ? statuses.findByStatusName(ElectionStatusName.SCHEDULED.name()).orElseThrow(() -> new IllegalStateException("Election status SCHEDULED is not seeded"))
                : requireStatus(request.statusId());
        if (request.statusId() != null && (request.sourceReference() == null || request.sourceReference().isBlank()))
            throw new IllegalArgumentException("Recording a supplied election status requires its source reference");
        if (request.sourceReference() != null && (request.sourceReference().isBlank() || request.sourceReference().length()>2048))
            throw new IllegalArgumentException("Source reference must be nonblank and at most 2048 characters");
        ElectionEvent event = electionMapper.toEntity(request, cycle, type, initial);
        if (request.geographySnapshotId() != null) {
            requirePublishedGeography(request.geographySnapshotId());
            event.assignGeography(request.geographySnapshotId());
        }
        event.recordSource(request.sourceReference());
        return electionMapper.toElectionEventDto(events.save(event));
    }

    @Transactional
    public Optional<ElectionEventDto> assignGeography(Long id, Long snapshotId) {
        requirePublishedGeography(snapshotId);
        return events.findById(id).map(event -> {
            event.assignGeography(snapshotId);
            return electionMapper.toElectionEventDto(events.save(event));
        });
    }

    private void requirePublishedGeography(Long id) {
        if (id == null || geography.find(id).filter(s -> s.getStatus() ==
                com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.SnapshotStatus.PUBLISHED).isEmpty())
            throw new IllegalArgumentException("Published election geography is required");
    }

    @Transactional
    public Optional<ElectionEventDto> changeStatus(Long id, ChangeElectionStatusRequest request) {
        if (request == null || request.statusId() == null) {
            throw new IllegalArgumentException("Election status is required");
        }
        return events.findById(id).map(event -> {
            boolean changed = event.changeStatus(requireStatus(request.statusId()));
            return electionMapper.toElectionEventDto(changed ? events.save(event) : event);
        });
    }

    private ElectionCycle requireCycle(Long id) {
        return cycles.findById(id).orElseThrow(() -> new IllegalArgumentException("Election cycle not found: " + id));
    }

    private ElectionType requireType(Long id) {
        return types.findById(id).orElseThrow(() -> new IllegalArgumentException("Election type not found: " + id));
    }

    private ElectionStatus requireStatus(Long id) {
        return statuses.findById(id).orElseThrow(() -> new IllegalArgumentException("Election status not found: " + id));
    }

    static PageRequest pageRequest(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be zero or greater");
        return PageRequest.ofPage(page + 1L).size(Math.max(1, Math.min(size, 500))).withoutTotal();
    }
}
