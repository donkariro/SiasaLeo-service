package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.DefectToPartyRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.JoinPartyRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PartyMembershipDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PartyMembership;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PartyMembershipRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
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
 * Party membership: enrol a member, move them to another party, and record a
 * resignation. Owns the invariant V16 delegates to the application — under the
 * Political Parties Act a person may belong to at most one party at a time —
 * and keeps closed memberships as history rather than overwriting them.
 * <p>
 * Membership ranges are inclusive of both dates, so a defection closes the old
 * membership the day <em>before</em> the new one starts: ending it on the same
 * day would leave the member in two parties for that day. That is why
 * defecting requires a date strictly after the current membership began, while
 * resigning may use the start date itself to record a single-day membership.
 */
@ApplicationScoped
public class PartyMembershipService {

    @Inject
    private PartyMembershipRepository memberships;

    @Inject
    private PersonRepository persons;

    @Inject
    private PoliticalPartyRepository politicalParties;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PartyMembershipDto join(JoinPartyRequest request) {
        Person person = requirePerson(request.personId());
        PoliticalParty party = requireParty(request.politicalPartyId());
        // The partial unique index is the real guarantee; this check turns the
        // common case into a friendly outcome and points at the right verb.
        if (findCurrent(person.getId()).isPresent()) {
            throw new IllegalArgumentException("Person " + person.getId()
                    + " already belongs to a party; defect or resign instead");
        }
        return PartyMembershipDto.from(memberships.save(
                new PartyMembership(person, party, dateOrToday(request.startDate()))));
    }

    @Transactional
    public PartyMembershipDto defect(Long personId, DefectToPartyRequest request) {
        Person person = requirePerson(personId);
        PoliticalParty party = requireParty(request.politicalPartyId());
        PartyMembership current = requireCurrent(person.getId());
        if (party.getId().equals(current.getPoliticalParty().getId())) {
            throw new IllegalArgumentException("Person " + person.getId()
                    + " already belongs to party " + party.getId());
        }
        LocalDate startDate = dateOrToday(request.startDate());
        if (!startDate.isAfter(current.getStartDate())) {
            throw new IllegalArgumentException("Defection date " + startDate
                    + " must fall after " + current.getStartDate()
                    + ", the day the current membership began");
        }

        current.end(startDate.minusDays(1));
        memberships.save(current);
        // Hibernate flushes inserts ahead of updates, so without forcing the
        // update out first the new open row would hit
        // idx_party_membership_current while the old one is still open.
        entityManager.flush();

        return PartyMembershipDto.from(memberships.save(
                new PartyMembership(person, party, startDate)));
    }

    @Transactional
    public PartyMembershipDto resign(Long personId, LocalDate endDate) {
        Person person = requirePerson(personId);
        PartyMembership current = requireCurrent(person.getId());
        LocalDate resignedOn = dateOrToday(endDate);
        if (resignedOn.isBefore(current.getStartDate())) {
            throw new IllegalArgumentException("Resignation date " + resignedOn
                    + " falls before " + current.getStartDate()
                    + ", the day the membership began");
        }
        current.end(resignedOn);
        return PartyMembershipDto.from(memberships.save(current));
    }

    public Optional<PartyMembershipDto> findById(Long id) {
        return memberships.findById(id).map(PartyMembershipDto::from);
    }

    /** The member's current membership, absent once they resign. */
    public Optional<PartyMembershipDto> findCurrentByPerson(Long personId) {
        requirePerson(personId);
        return findCurrent(personId).map(PartyMembershipDto::from);
    }

    /** Every membership the person has held, the most recent first. */
    public List<PartyMembershipDto> findHistory(Long personId) {
        requirePerson(personId);
        return memberships.findByPerson(personId).stream()
                .map(PartyMembershipDto::from)
                .toList();
    }

    /** A party's current membership roll; closed memberships are left out. */
    public List<PartyMembershipDto> findByParty(Long partyId, int page, int size) {
        requireParty(partyId);
        return memberships.findCurrentByParty(partyId, pageRequest(page, size)).stream()
                .map(PartyMembershipDto::from)
                .toList();
    }

    private Person requirePerson(Long personId) {
        return persons.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));
    }

    private PoliticalParty requireParty(Long partyId) {
        return politicalParties.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Political party not found: " + partyId));
    }

    private PartyMembership requireCurrent(Long personId) {
        return findCurrent(personId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Person " + personId + " does not belong to any party"));
    }

    private Optional<PartyMembership> findCurrent(Long personId) {
        return memberships.findCurrentByPerson(personId);
    }

    private static LocalDate dateOrToday(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
