package com.arriyiaconsulting.siasaleo.service.domain.voter.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.ElectoralAreaRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.RegisterVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.TransferVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
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
import java.util.List;
import java.util.Optional;

/**
 * Voter-roll operations: register a voter at a centre, transfer them to
 * another, and deregister them. Owns the two invariants V15 delegates to the
 * application — the centre must be an area of type REGISTRATION_CENTER, and a
 * person holds at most one ACTIVE registration — and keeps retired rows as
 * history rather than overwriting them.
 */
@ApplicationScoped
public class VoterRegistrationService {

    // Registration is per centre, not per polling station: stations are the
    // election-day streams a centre is split into (see V15).
    static final String REGISTRATION_CENTER = "REGISTRATION_CENTER";

    @Inject
    private VoterRegistrationRepository registrations;

    @Inject
    private PersonRepository persons;

    @Inject
    private ElectoralAreaRepository electoralAreas;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public VoterRegistrationDto register(RegisterVoterRequest request) {
        Person person = requirePerson(request.personId());
        ElectoralArea center = requireRegistrationCenter(request.registrationCenterId());
        // The partial unique index is the real guarantee; this check turns the
        // common case into a friendly outcome and points at the right verb.
        if (findActive(person.getId()).isPresent()) {
            throw new IllegalArgumentException("Person " + person.getId()
                    + " already has an active voter registration; transfer it instead");
        }
        return VoterRegistrationDto.from(registrations.save(
                new VoterRegistration(person, center, dateOrToday(request.registrationDate()))));
    }

    @Transactional
    public VoterRegistrationDto transfer(Long personId, TransferVoterRequest request) {
        Person person = requirePerson(personId);
        ElectoralArea center = requireRegistrationCenter(request.registrationCenterId());
        VoterRegistration current = requireActive(person.getId());
        if (center.getId().equals(current.getRegistrationCenter().getId())) {
            throw new IllegalArgumentException("Person " + person.getId()
                    + " is already registered at centre " + center.getId());
        }

        current.transferAway();
        registrations.save(current);
        // Hibernate flushes inserts ahead of updates, so without forcing the
        // update out first the new ACTIVE row would hit
        // idx_voter_registration_active while the old one is still ACTIVE.
        entityManager.flush();

        return VoterRegistrationDto.from(registrations.save(
                new VoterRegistration(person, center, dateOrToday(request.registrationDate()))));
    }

    @Transactional
    public VoterRegistrationDto deregister(Long personId) {
        Person person = requirePerson(personId);
        VoterRegistration current = requireActive(person.getId());
        current.deregister();
        return VoterRegistrationDto.from(registrations.save(current));
    }

    public Optional<VoterRegistrationDto> findById(Long id) {
        return registrations.findById(id).map(VoterRegistrationDto::from);
    }

    /** The voter's current registration, absent once they deregister. */
    public Optional<VoterRegistrationDto> findCurrent(Long personId) {
        requirePerson(personId);
        return findActive(personId).map(VoterRegistrationDto::from);
    }

    /** Every registration the voter has held, current one first. */
    public List<VoterRegistrationDto> findHistory(Long personId) {
        requirePerson(personId);
        return registrations.findByPerson(personId).stream()
                .map(VoterRegistrationDto::from)
                .toList();
    }

    /** The active roll for a centre; retired registrations are left out. */
    public List<VoterRegistrationDto> findByCenter(Long centerId, int page, int size) {
        requireRegistrationCenter(centerId);
        return registrations.findByCenterAndStatus(
                        centerId, VoterRegistrationStatus.ACTIVE, pageRequest(page, size)).stream()
                .map(VoterRegistrationDto::from)
                .toList();
    }

    private Person requirePerson(Long personId) {
        return persons.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));
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

    private VoterRegistration requireActive(Long personId) {
        return findActive(personId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Person " + personId + " has no active voter registration"));
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
