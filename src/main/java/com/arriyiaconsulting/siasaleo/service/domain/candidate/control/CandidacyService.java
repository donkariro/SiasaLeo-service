package com.arriyiaconsulting.siasaleo.service.domain.candidate.control;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.CandidacyDto;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.RegisterCandidateRequest;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.Candidacy;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.CandidacyStatus;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.repository.CandidacyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.repository.CandidacyStatusRepository;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.ContestRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Candidate registration: records a person's entry into a contest, creating
 * the person on the fly when they are not yet in the register. Every new
 * candidacy starts at EXPRESSED_INTEREST, the first stage of the V21
 * lifecycle; moving it through later stages is separate functionality.
 */
@ApplicationScoped
public class CandidacyService {

    // First stage of the candidacy lifecycle seeded by V21.
    static final String INITIAL_STATUS = "EXPRESSED_INTEREST";

    @Inject
    private CandidacyRepository candidacies;

    @Inject
    private CandidacyStatusRepository statuses;

    @Inject
    private PersonRepository persons;

    @Inject
    private ContestRepository contests;

    @Inject
    private PoliticalPartyRepository politicalParties;

    @Transactional
    public CandidacyDto register(RegisterCandidateRequest request) {
        if ((request.personId() == null) == (request.person() == null)) {
            throw new IllegalArgumentException(
                    "Provide exactly one of personId or person");
        }
        Contest contest = contests.findById(request.contestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contest not found: " + request.contestId()));
        PoliticalParty party = null;
        if (request.politicalPartyId() != null) {
            party = politicalParties.findById(request.politicalPartyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Political party not found: " + request.politicalPartyId()));
        }
        Person person = resolvePerson(request, contest);
        // A missing seed row is a deployment defect, not a caller error, so
        // this surfaces as a 500 rather than a 400.
        CandidacyStatus initialStatus = statuses.findByStatusName(INITIAL_STATUS)
                .orElseThrow(() -> new IllegalStateException(
                        "Candidacy status " + INITIAL_STATUS + " is not seeded"));

        Candidacy candidacy = candidacies.save(
                new Candidacy(person, contest, party, initialStatus));
        return CandidacyDto.from(candidacy);
    }

    private Person resolvePerson(RegisterCandidateRequest request, Contest contest) {
        if (request.personId() == null) {
            // A person created here cannot already be in the contest, so no
            // duplicate check is needed.
            RegisterCandidateRequest.NewPerson details = request.person();
            return persons.save(new Person(details.firstName(), details.lastName(),
                    details.dateOfBirth(), details.gender()));
        }
        Person person = persons.findById(request.personId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Person not found: " + request.personId()));
        // The unique (person_id, contest_id) constraint is the real
        // guarantee; this check just turns the common case into a friendly
        // outcome.
        if (candidacies.findByPersonAndContest(person.getId(), contest.getId()).isPresent()) {
            throw new IllegalArgumentException("Person " + person.getId()
                    + " is already registered in contest " + contest.getId());
        }
        return person;
    }

    public Optional<CandidacyDto> findById(Long id) {
        return candidacies.findById(id).map(CandidacyDto::from);
    }

    public List<CandidacyDto> findByContest(Long contestId, int page, int size) {
        contests.findById(contestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contest not found: " + contestId));
        return candidacies.findByContest(contestId, pageRequest(page, size)).stream()
                .map(CandidacyDto::from)
                .toList();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
