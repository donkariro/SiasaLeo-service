package com.arriyiaconsulting.siasaleo.service.domain.election.control;

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
    @Inject private ElectionCycleRepository cycles;
    @Inject private ElectionTypeRepository types;
    @Inject private ElectionStatusRepository statuses;
    @Inject private ElectionEventRepository events;

    public List<ElectionCycleDto> cycles() {
        return cycles.findAllOrdered().stream().map(ElectionCycleDto::from).toList();
    }

    public Optional<ElectionCycleDto> findCycle(Long id) {
        return cycles.findById(id).map(ElectionCycleDto::from);
    }

    public List<ElectionTypeDto> types() {
        return types.findAllOrdered().stream().map(ElectionTypeDto::from).toList();
    }

    public List<ElectionStatusDto> statuses() {
        return statuses.findAllOrdered().stream().map(ElectionStatusDto::from).toList();
    }

    public Optional<ElectionEventDto> findEvent(Long id) {
        return events.findById(id).map(ElectionEventDto::from);
    }

    public List<ElectionEventDto> events(Long cycleId, Long typeId, Long statusId, int page, int size) {
        PageRequest paging = pageRequest(page, size);
        if (cycleId != null) requireCycle(cycleId);
        if (typeId != null) requireType(typeId);
        if (statusId != null) requireStatus(statusId);
        return events.search(cycleId, typeId, statusId, paging).stream().map(ElectionEventDto::from).toList();
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
        ElectionStatus scheduled = statuses.findByStatusName("SCHEDULED")
                .orElseThrow(() -> new IllegalStateException("Election status SCHEDULED is not seeded"));
        return ElectionEventDto.from(events.save(
                new ElectionEvent(cycle, request.electionDate(), type, scheduled)));
    }

    @Transactional
    public Optional<ElectionEventDto> changeStatus(Long id, ChangeElectionStatusRequest request) {
        if (request == null || request.statusId() == null) {
            throw new IllegalArgumentException("Election status is required");
        }
        return events.findById(id).map(event -> {
            ElectionStatus next = requireStatus(request.statusId());
            String current = event.getStatus().getStatusName();
            String target = next.getStatusName();
            if (current.equals(target)) return ElectionEventDto.from(event);
            boolean allowed = switch (current) {
                case "SCHEDULED" -> target.equals("ONGOING") || target.equals("CANCELLED");
                case "ONGOING" -> target.equals("COMPLETED") || target.equals("NULLIFIED");
                case "COMPLETED" -> target.equals("NULLIFIED");
                default -> false;
            };
            if (!allowed) {
                throw new IllegalArgumentException("Cannot change election status from " + current + " to " + target);
            }
            event.setStatus(next);
            return ElectionEventDto.from(events.save(event));
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
