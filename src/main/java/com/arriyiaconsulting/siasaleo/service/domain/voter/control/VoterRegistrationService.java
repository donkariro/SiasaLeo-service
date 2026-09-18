package com.arriyiaconsulting.siasaleo.service.domain.voter.control;

import com.arriyiaconsulting.siasaleo.service.domain.voter.mapping.VoterMapper;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.ElectoralAreaRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.RegisterVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.TransferVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDetails;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistration;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistrationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.voter.repository.VoterRegistrationRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * Voter-roll operations: a user declares themselves a voter at a centre,
 * transfers to another, or withdraws. Owns the two invariants V15 delegates to
 * the application — the centre must be an area of type REGISTRATION_CENTER,
 * and a person holds at most one ACTIVE registration — and keeps retired rows
 * as history rather than overwriting them.
 *
 * Registrations here are self-declared, not a mirror of the IEBC roll: they
 * exist so the system can scope what it shows a user to the seats they
 * actually vote for. Self-service writes are addressed by the *account*, and
 * the person comes from PersonProfileService rather than from the caller — an
 * id taken from a path or body would let anyone register, move or withdraw
 * anyone else. Declaring as a voter is usually what creates the person, which
 * is why register carries the profile details.
 */
@ApplicationScoped
public class VoterRegistrationService {

    @Inject
    private VoterMapper voterMapper;

    // Registration is per centre, not per polling station: stations are the
    // election-day streams a centre is split into (see V15).
    static final String REGISTRATION_CENTER = "REGISTRATION_CENTER";

    private static final int VOTING_AGE = 18;

    @Inject
    private VoterRegistrationRepository registrations;

    @Inject
    private PersonProfileService profiles;

    @Inject
    private ElectoralAreaRepository electoralAreas;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Declares the caller a voter, creating their person from the request's
     * profile if this is their first role. The person and the registration are
     * written in one transaction, so a rejected registration leaves no person
     * stranded behind it.
     */
    @Transactional
    public VoterRegistrationDto register(Long accountId, RegisterVoterRequest request) {
        ElectoralArea center = requireRegistrationCenter(request.registrationCenterId());
        Person person = profiles.resolveFor(accountId, request.profile());
        LocalDate registeredOn = dateOrToday(request.registrationDate());
        requireVotingAge(person, registeredOn);
        // The partial unique index is the real guarantee; this check turns the
        // common case into a friendly outcome and points at the right verb.
        if (findActive(person.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "You already have an active voter registration; transfer it instead");
        }
        return voterMapper.toVoterRegistrationDto(registrations.save(
                voterMapper.toEntity(person, center, registeredOn)));
    }

    /**
     * Used by candidate self-registration after resolving the authenticated
     * account's person. Requires the caller's transaction so a failed
     * candidacy cannot leave a voter registration behind.
     * Existing active registrations are reused, never moved or overwritten.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void ensureActiveForCandidate(Person person, VoterRegistrationDetails details) {
        if (findActive(person.getId()).isPresent()) {
            return;
        }
        if (details == null || details.registrationCenterId() == null) {
            throw new IllegalArgumentException("Candidate must have an active voter registration; "
                    + "provide voterRegistration.registrationCenterId to register during candidate registration");
        }
        if (details.registrationCenterId() <= 0) {
            throw new IllegalArgumentException("Registration centre ID must be positive");
        }
        LocalDate registeredOn = dateOrToday(details.registrationDate());
        if (registeredOn.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Voter registration date must not be in the future");
        }
        ElectoralArea center = requireRegistrationCenter(details.registrationCenterId());
        requireVotingAge(person, registeredOn);
        // The existing partial unique index guarantees at most one ACTIVE row.
        registrations.save(voterMapper.toEntity(person, center, registeredOn));
    }

    @Transactional
    public VoterRegistrationDto transfer(Long accountId, TransferVoterRequest request) {
        Person person = requirePersonFor(accountId);
        ElectoralArea center = requireRegistrationCenter(request.registrationCenterId());
        VoterRegistration current = requireActive(person.getId());
        if (center.getId().equals(current.getRegistrationCenter().getId())) {
            throw new IllegalArgumentException(
                    "You are already registered at centre " + center.getId());
        }

        current.transferAway();
        registrations.save(current);
        // Hibernate flushes inserts ahead of updates, so without forcing the
        // update out first the new ACTIVE row would hit
        // idx_voter_registration_active while the old one is still ACTIVE.
        entityManager.flush();

        return voterMapper.toVoterRegistrationDto(registrations.save(
                voterMapper.toEntity(person, center, dateOrToday(request.registrationDate()))));
    }

    @Transactional
    public VoterRegistrationDto deregister(Long accountId) {
        Person person = requirePersonFor(accountId);
        VoterRegistration current = requireActive(person.getId());
        current.deregister();
        return voterMapper.toVoterRegistrationDto(registrations.save(current));
    }

    public Optional<VoterRegistrationDto> findById(Long id) {
        return registrations.findById(id).map(voterMapper::toVoterRegistrationDto);
    }

    /** The caller's current registration, absent until they declare one. */
    public Optional<VoterRegistrationDto> findCurrentFor(Long accountId) {
        return profiles.findFor(accountId)
                .flatMap(person -> findActive(person.getId()))
                .map(voterMapper::toVoterRegistrationDto);
    }

    /** Every registration the caller has held, current one first. */
    public List<VoterRegistrationDto> findHistoryFor(Long accountId) {
        return profiles.findFor(accountId)
                .map(person -> findHistory(person.getId()))
                .orElseGet(List::of);
    }

    /** Another person's current registration — an administrative read. */
    public Optional<VoterRegistrationDto> findCurrent(Long personId) {
        return findActive(personId).map(voterMapper::toVoterRegistrationDto);
    }

    /** Another person's registration trail — an administrative read. */
    public List<VoterRegistrationDto> findHistory(Long personId) {
        return registrations.findByPerson(personId).stream()
                .map(voterMapper::toVoterRegistrationDto)
                .toList();
    }

    /** The active roll for a centre; retired registrations are left out. */
    public List<VoterRegistrationDto> findByCenter(Long centerId, int page, int size) {
        requireRegistrationCenter(centerId);
        return registrations.findByCenterAndStatus(
                        centerId, VoterRegistrationStatus.ACTIVE, pageRequest(page, size)).stream()
                .map(voterMapper::toVoterRegistrationDto)
                .toList();
    }

    /**
     * The caller's person, which exists only once they have declared a role.
     * Transfer and withdrawal both presuppose a registration, so there is
     * nothing sensible to create here.
     */
    private Person requirePersonFor(Long accountId) {
        return profiles.findFor(accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "You are not registered as a voter yet"));
    }

    private ElectoralArea requireRegistrationCenter(Long centerId) {
        ElectoralArea area = electoralAreas.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Electoral area not found: " + centerId));
        if (!REGISTRATION_CENTER.equals(area.getAreaType().getName())) {
            throw new IllegalArgumentException("Area " + centerId + " is a "
                    + area.getAreaType().getName() + ", not a " + REGISTRATION_CENTER);
        }
        return area;
    }

    /**
     * Nothing verifies a self-declared date of birth, so this is not proof of
     * eligibility — it only keeps the obviously ineligible off the roll, and
     * only when a date was given. ProfileDetails leaves the date optional and
     * defers to the role; this is that decision for voters.
     */
    private static void requireVotingAge(Person person, LocalDate registeredOn) {
        LocalDate dateOfBirth = person.getDateOfBirth();
        if (dateOfBirth != null
                && Period.between(dateOfBirth, registeredOn).getYears() < VOTING_AGE) {
            throw new IllegalArgumentException(
                    "A voter must be " + VOTING_AGE + " by the date of registration");
        }
    }

    private VoterRegistration requireActive(Long personId) {
        return findActive(personId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "You have no active voter registration"));
    }

    private Optional<VoterRegistration> findActive(Long personId) {
        return registrations.findByPersonAndStatus(personId, VoterRegistrationStatus.ACTIVE);
    }

    private static LocalDate dateOrToday(LocalDate registrationDate) {
        return registrationDate != null ? registrationDate : LocalDate.now();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
