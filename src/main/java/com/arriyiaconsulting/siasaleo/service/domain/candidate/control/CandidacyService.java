package com.arriyiaconsulting.siasaleo.service.domain.candidate.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.mapping.PersonMapper;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.mapping.CandidacyMapper;
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
import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.CandidateRegistrationFormDto;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
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

    @Inject
    private PersonMapper personMapper;

    @Inject
    private CandidacyMapper candidacyMapper;

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

    @Inject
    private VoterRegistrationService voters;

    @Inject
    private PersonProfileService profiles;

    /** Load the signed-in user's shared voter fields before rendering the form. */
    public CandidateRegistrationFormDto registrationForm(Long accountId) {
        Optional<Person> person = profiles.findFor(accountId);
        Optional<VoterRegistrationDto> registration = person.flatMap(p -> voters.findCurrent(p.getId()));
        ProfileDetails profile = person.map(personMapper::toProfileDetails).orElse(null);
        return new CandidateRegistrationFormDto(registration.isPresent(), registration.isPresent(),
                profile, registration.orElse(null));
    }

    @Transactional
    public CandidacyDto register(Long accountId, RegisterCandidateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Candidate registration is required");
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
        Person person = profiles.resolveFor(accountId, request.profile());
        if (candidacies.findByPersonAndContest(person.getId(), contest.getId()).isPresent()) {
            throw new IllegalArgumentException("You are already registered in contest " + contest.getId());
        }
        // A missing seed row is a deployment defect, not a caller error, so
        // this surfaces as a 500 rather than a 400.
        CandidacyStatus initialStatus = statuses.findByStatusName(INITIAL_STATUS)
                .orElseThrow(() -> new IllegalStateException(
                        "Candidacy status " + INITIAL_STATUS + " is not seeded"));

        // Recheck at submission; an earlier form response is not authoritative.
        // Existing voters cannot rewrite shared fields through this form.
        if (voters.findCurrent(person.getId()).isEmpty() && request.profile() != null) {
            ProfileDetails details = request.profile();
            person.updateDetails(details.firstName().trim(), details.lastName().trim(),
                    details.dateOfBirth(), details.gender());
            person = persons.save(person);
        }
        // Joins this transaction: person, voter registration and candidacy
        // either all succeed or all roll back.
        voters.ensureActiveForCandidate(person, request.voterRegistration());

        Candidacy candidacy = candidacies.save(
                candidacyMapper.toEntity(person, contest, party, initialStatus));
        return candidacyMapper.toCandidacyDto(candidacy);
    }

    public Optional<CandidacyDto> findById(Long id) {
        return candidacies.findById(id).map(candidacyMapper::toCandidacyDto);
    }

    @Transactional
    public Optional<CandidacyDto> recordBallot(Long id, com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.RecordBallotRequest r) {
        return candidacies.findById(id).map(c -> {
            c.recordBallot(r.ballotName(),r.ballotPartyName(),r.sourceReference(),r.sourceRecordReference(),null);
            return candidacyMapper.toCandidacyDto(candidacies.save(c));
        });
    }

    /** Administrative capture for any election, without creating a user registration. */
    @Transactional
    public CandidacyDto importCandidacy(com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.ImportCandidacyRequest r) {
        if(r==null || r.contestId()==null || r.statusId()==null || r.sourceReference()==null || r.sourceReference().isBlank()
                || r.sourceRecordReference()==null || r.sourceRecordReference().isBlank()) throw new IllegalArgumentException("Contest, status and source record are required");
        String fingerprint=com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.fingerprint(r);
        Optional<Candidacy> existing=candidacies.findBySource(r.contestId(),r.sourceReference(),r.sourceRecordReference());
        if(existing.isPresent()) {
            if(!fingerprint.equals(existing.get().getImportFingerprint())) throw new IllegalArgumentException("Source record already has a different candidacy payload");
            return candidacyMapper.toCandidacyDto(existing.get());
        }
        Contest contest=contests.findById(r.contestId()).orElseThrow(()->new IllegalArgumentException("Contest not found"));
        CandidacyStatus status=statuses.findById(r.statusId()).orElseThrow(()->new IllegalArgumentException("Candidacy status not found"));
        PoliticalParty party=r.politicalPartyId()==null ? null : politicalParties.findById(r.politicalPartyId()).orElseThrow(()->new IllegalArgumentException("Political party not found"));
        Person person;
        if(r.personId()!=null) {
            if(r.profile()!=null) throw new IllegalArgumentException("Supply either an existing person ID or a new profile");
            person=persons.findById(r.personId()).orElseThrow(()->new IllegalArgumentException("Person not found"));
            if(candidacies.findByPersonAndContest(person.getId(),contest.getId()).isPresent()) throw new IllegalArgumentException("Person already has a candidacy in this contest");
        } else {
            ProfileDetails p=r.profile();
            if(p==null || p.firstName()==null || p.firstName().isBlank() || p.lastName()==null || p.lastName().isBlank()) throw new IllegalArgumentException("A new person requires first and last names");
            person=persons.save(new Person(p.firstName().trim(),p.lastName().trim(),p.dateOfBirth(),p.gender()));
        }
        Candidacy candidacy=new Candidacy(person,contest,party,status);
        candidacy.recordBallot(r.ballotName(),r.ballotPartyName(),r.sourceReference(),r.sourceRecordReference(),fingerprint);
        return candidacyMapper.toCandidacyDto(candidacies.save(candidacy));
    }

    public List<CandidacyDto> findByContest(Long contestId, int page, int size) {
        contests.findById(contestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contest not found: " + contestId));
        return candidacies.findByContest(contestId, pageRequest(page, size)).stream()
                .map(candidacyMapper::toCandidacyDto)
                .toList();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
