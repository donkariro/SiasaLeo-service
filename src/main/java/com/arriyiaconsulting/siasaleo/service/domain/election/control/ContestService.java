package com.arriyiaconsulting.siasaleo.service.domain.election.control;

import com.arriyiaconsulting.siasaleo.service.domain.election.mapping.ElectionMapper;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.SeatRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ContestService {

    @Inject
    private ElectionMapper electionMapper;
    @Inject private ContestRepository contests;
    @Inject private ElectionEventRepository events;
    @Inject private SeatRepository seats;
    @Inject private ContestJurisdictionService jurisdictions;

    public Optional<ContestDto> findById(Long id) {
        return contests.findById(id).map(electionMapper::toContestDto);
    }

    public List<ContestDto> findByEvent(Long eventId, Long seatId, int page, int size) {
        PageRequest paging = ElectionService.pageRequest(page, size);
        if (eventId == null) throw new IllegalArgumentException("Election event is required");
        events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Election event not found: " + eventId));
        if (seatId != null) {
            seats.findById(seatId).orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
        }
        return contests.search(eventId, seatId, paging).stream().map(electionMapper::toContestDto).toList();
    }

    @Transactional
    public ContestDto create(CreateContestRequest request) {
        if (request == null || request.electionEventId() == null ||
                (request.seatId() == null && (request.officeId() == null || request.jurisdictionId() == null))) {
            throw new IllegalArgumentException("Election event and either a seat or office/jurisdiction are required");
        }
        if (request.description() != null && request.description().length() > 255) {
            throw new IllegalArgumentException("Description must not exceed 255 characters");
        }
        var event = events.findById(request.electionEventId())
                .orElseThrow(() -> new IllegalArgumentException("Election event not found: " + request.electionEventId()));
        if (request.seatId() != null) seats.findById(request.seatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + request.seatId()));
        // V8's unique constraint also protects concurrent inserts.
        if (request.seatId() != null && contests.findByEventAndSeat(request.electionEventId(), request.seatId()).isPresent()) {
            throw new IllegalArgumentException("A contest already exists for this election event and seat");
        }
        String description = request.description() == null || request.description().isBlank()
                ? null : request.description().trim();
        var contest = electionMapper.toEntity(request, description);
        if (request.jurisdictionId() != null || request.officeId() != null) {
            jurisdictions.assign(contest, event, request.officeId(), request.jurisdictionId());
        }
        return electionMapper.toContestDto(contests.save(contest));
    }

    @Transactional
    public Optional<ContestDto> assignJurisdiction(Long id, Long office, Long area) {
        return contests.findById(id).map(contest -> {
            var event = events.findById(contest.getElectionEventId()).orElseThrow();
            jurisdictions.assign(contest, event, office, area);
            return electionMapper.toContestDto(contests.save(contest));
        });
    }
}
