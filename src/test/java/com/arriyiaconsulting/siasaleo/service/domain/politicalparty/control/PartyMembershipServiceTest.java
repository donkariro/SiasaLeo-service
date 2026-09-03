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
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the invariant V16 delegates to the application: a person
 * belongs to at most one party at a time. Closing a membership must keep it as
 * history rather than overwriting it, and consecutive memberships must not
 * overlap — ranges being inclusive of both dates, a defection ends the old
 * membership the day before the new one begins.
 */
@ExtendWith(MockitoExtension.class)
class PartyMembershipServiceTest {

    private static final long PERSON_ID = 5L;
    private static final long PARTY_ID = 3L;
    private static final long OTHER_PARTY_ID = 7L;

    @Mock
    private PartyMembershipRepository memberships;

    @Mock
    private PersonRepository persons;

    @Mock
    private PoliticalPartyRepository politicalParties;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PartyMembershipService service;

    @Test
    void joinRecordsTheMemberAgainstTheParty() {
        givenPerson();
        givenParty(PARTY_ID, "Orange Democratic Movement", "ODM");
        givenNoCurrentMembership();
        savePassesThrough();

        PartyMembershipDto joined = service.join(
                new JoinPartyRequest(PERSON_ID, PARTY_ID, null));

        assertEquals(PERSON_ID, joined.personId());
        assertEquals(PARTY_ID, joined.politicalPartyId());
        assertEquals("Orange Democratic Movement", joined.partyName());
        assertEquals("ODM", joined.partyAbbreviation());
        assertNull(joined.endDate());
        assertTrue(joined.current());
    }

    @Test
    void joinDefaultsTheStartDateToToday() {
        givenPerson();
        givenParty(PARTY_ID, "Orange Democratic Movement", "ODM");
        givenNoCurrentMembership();
        savePassesThrough();

        service.join(new JoinPartyRequest(PERSON_ID, PARTY_ID, null));

        ArgumentCaptor<PartyMembership> saved = ArgumentCaptor.forClass(PartyMembership.class);
        verify(memberships).save(saved.capture());
        assertEquals(LocalDate.now(), saved.getValue().getStartDate());
    }

    @Test
    void joinKeepsABackfilledStartDate() {
        givenPerson();
        givenParty(PARTY_ID, "Orange Democratic Movement", "ODM");
        givenNoCurrentMembership();
        savePassesThrough();

        LocalDate joinedOn = LocalDate.of(2017, 4, 26);
        PartyMembershipDto joined = service.join(
                new JoinPartyRequest(PERSON_ID, PARTY_ID, joinedOn));

        assertEquals(joinedOn, joined.startDate());
    }

    @Test
    void joinRejectsASecondMembershipAndPointsAtTheRightVerbs() {
        givenPerson();
        givenParty(PARTY_ID, "Orange Democratic Movement", "ODM");
        givenCurrentMembership(PARTY_ID, LocalDate.of(2017, 4, 26));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.join(new JoinPartyRequest(PERSON_ID, PARTY_ID, null)));

        assertTrue(thrown.getMessage().contains("defect or resign instead"));
        verify(memberships, never()).save(any());
    }

    @Test
    void joinRejectsUnknownPerson() {
        when(persons.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.join(new JoinPartyRequest(999L, PARTY_ID, null)));

        assertTrue(thrown.getMessage().contains("Person not found"));
        verify(memberships, never()).save(any());
    }

    @Test
    void joinRejectsUnknownParty() {
        givenPerson();
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.join(new JoinPartyRequest(PERSON_ID, 999L, null)));

        assertTrue(thrown.getMessage().contains("Political party not found"));
        verify(memberships, never()).save(any());
    }

    @Test
    void defectClosesTheOldMembershipTheDayBeforeTheNewOneBegins() {
        givenPerson();
        givenParty(OTHER_PARTY_ID, "United Democratic Alliance", "UDA");
        PartyMembership current = givenCurrentMembership(PARTY_ID, LocalDate.of(2017, 4, 26));
        savePassesThrough();

        LocalDate defectedOn = LocalDate.of(2022, 5, 9);
        PartyMembershipDto moved = service.defect(
                PERSON_ID, new DefectToPartyRequest(OTHER_PARTY_ID, defectedOn));

        // Inclusive ranges: ending on the day the new membership starts would
        // put the member in two parties for a day, which the Act forbids.
        assertEquals(defectedOn.minusDays(1), current.getEndDate());
        assertFalse(current.isCurrent());
        assertEquals(OTHER_PARTY_ID, moved.politicalPartyId());
        assertEquals(defectedOn, moved.startDate());
        assertTrue(moved.current());
    }

    @Test
    void defectFlushesTheClosureBeforeInsertingTheNewMembership() {
        givenPerson();
        givenParty(OTHER_PARTY_ID, "United Democratic Alliance", "UDA");
        PartyMembership current = givenCurrentMembership(PARTY_ID, LocalDate.of(2017, 4, 26));
        savePassesThrough();

        service.defect(PERSON_ID,
                new DefectToPartyRequest(OTHER_PARTY_ID, LocalDate.of(2022, 5, 9)));

        // The update closing the old row has to reach the database before the
        // new open row is inserted, or the partial unique index
        // idx_party_membership_current rejects it.
        ArgumentCaptor<PartyMembership> saved = ArgumentCaptor.forClass(PartyMembership.class);
        InOrder ordered = inOrder(memberships, entityManager);
        ordered.verify(memberships).save(current);
        ordered.verify(entityManager).flush();
        ordered.verify(memberships).save(saved.capture());
        assertTrue(saved.getValue().isCurrent());
    }

    @Test
    void defectRejectsThePartyTheMemberAlreadyBelongsTo() {
        givenPerson();
        givenParty(PARTY_ID, "Orange Democratic Movement", "ODM");
        givenCurrentMembership(PARTY_ID, LocalDate.of(2017, 4, 26));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.defect(PERSON_ID, new DefectToPartyRequest(PARTY_ID, null)));

        assertTrue(thrown.getMessage().contains("already belongs to party"));
        verify(memberships, never()).save(any());
        verify(entityManager, never()).flush();
    }

    // Closing the old membership the day before such a date would either
    // breach the V16 CHECK (end_date >= start_date) or leave the two
    // memberships overlapping.
    @Test
    void defectRejectsADateTheCurrentMembershipAlreadyCovers() {
        givenPerson();
        givenParty(OTHER_PARTY_ID, "United Democratic Alliance", "UDA");
        LocalDate startDate = LocalDate.of(2017, 4, 26);
        givenCurrentMembership(PARTY_ID, startDate);

        IllegalArgumentException sameDay = assertThrows(IllegalArgumentException.class,
                () -> service.defect(PERSON_ID,
                        new DefectToPartyRequest(OTHER_PARTY_ID, startDate)));
        IllegalArgumentException earlier = assertThrows(IllegalArgumentException.class,
                () -> service.defect(PERSON_ID,
                        new DefectToPartyRequest(OTHER_PARTY_ID, startDate.minusDays(1))));

        assertTrue(sameDay.getMessage().contains("must fall after"));
        assertTrue(earlier.getMessage().contains("must fall after"));
        verify(memberships, never()).save(any());
        verify(entityManager, never()).flush();
    }

    @Test
    void defectRequiresACurrentMembership() {
        givenPerson();
        givenParty(OTHER_PARTY_ID, "United Democratic Alliance", "UDA");
        givenNoCurrentMembership();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.defect(PERSON_ID, new DefectToPartyRequest(OTHER_PARTY_ID, null)));

        assertTrue(thrown.getMessage().contains("does not belong to any party"));
        verify(memberships, never()).save(any());
    }

    @Test
    void resignClosesTheCurrentMembershipOnTheGivenDay() {
        givenPerson();
        PartyMembership current = givenCurrentMembership(PARTY_ID, LocalDate.of(2017, 4, 26));
        savePassesThrough();

        LocalDate resignedOn = LocalDate.of(2021, 8, 1);
        PartyMembershipDto resigned = service.resign(PERSON_ID, resignedOn);

        assertEquals(resignedOn, current.getEndDate());
        assertEquals(resignedOn, resigned.endDate());
        assertFalse(resigned.current());
        verify(memberships).save(current);
    }

    @Test
    void resignDefaultsToToday() {
        givenPerson();
        PartyMembership current = givenCurrentMembership(PARTY_ID, LocalDate.now().minusYears(1));
        savePassesThrough();

        service.resign(PERSON_ID, null);

        assertEquals(LocalDate.now(), current.getEndDate());
    }

    // The V16 CHECK allows end_date = start_date, so a membership joined and
    // given up the same day is legitimate — unlike a same-day defection.
    @Test
    void resignAcceptsASingleDayMembership() {
        givenPerson();
        LocalDate startDate = LocalDate.of(2021, 8, 1);
        PartyMembership current = givenCurrentMembership(PARTY_ID, startDate);
        savePassesThrough();

        service.resign(PERSON_ID, startDate);

        assertEquals(startDate, current.getEndDate());
    }

    @Test
    void resignRejectsADateBeforeTheMembershipBegan() {
        givenPerson();
        LocalDate startDate = LocalDate.of(2021, 8, 1);
        givenCurrentMembership(PARTY_ID, startDate);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.resign(PERSON_ID, startDate.minusDays(1)));

        assertTrue(thrown.getMessage().contains("falls before"));
        verify(memberships, never()).save(any());
    }

    @Test
    void resignRequiresACurrentMembership() {
        givenPerson();
        givenNoCurrentMembership();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.resign(PERSON_ID, null));

        assertTrue(thrown.getMessage().contains("does not belong to any party"));
        verify(memberships, never()).save(any());
    }

    @Test
    void currentMembershipIsEmptyOnceResigned() {
        givenPerson();
        givenNoCurrentMembership();

        assertTrue(service.findCurrentByPerson(PERSON_ID).isEmpty());
    }

    @Test
    void historyKeepsClosedMembershipsAlongsideTheCurrentOne() {
        givenPerson();
        PartyMembership closed = new PartyMembership(personEntity(),
                partyEntity(PARTY_ID, "Orange Democratic Movement", "ODM"),
                LocalDate.of(2017, 4, 26));
        closed.end(LocalDate.of(2022, 5, 8));
        PartyMembership open = new PartyMembership(personEntity(),
                partyEntity(OTHER_PARTY_ID, "United Democratic Alliance", "UDA"),
                LocalDate.of(2022, 5, 9));
        when(memberships.findByPerson(PERSON_ID)).thenReturn(List.of(open, closed));

        List<PartyMembershipDto> history = service.findHistory(PERSON_ID);

        assertEquals(2, history.size());
        assertTrue(history.get(0).current());
        assertFalse(history.get(1).current());
        assertEquals(LocalDate.of(2022, 5, 8), history.get(1).endDate());
    }

    @Test
    void partyRollListsOnlyCurrentMembersAndConvertsPaging() {
        givenParty(PARTY_ID, "Orange Democratic Movement", "ODM");
        when(memberships.findCurrentByParty(anyLong(), any(PageRequest.class)))
                .thenReturn(List.of());

        service.findByParty(PARTY_ID, 0, 20);

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(memberships).findCurrentByParty(anyLong(), pageRequest.capture());
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(1L, pageRequest.getValue().page());
        assertEquals(20, pageRequest.getValue().size());
    }

    @Test
    void partyRollRejectsAnUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findByParty(999L, 0, 20));
        verify(memberships, never()).findCurrentByParty(anyLong(), any(PageRequest.class));
    }

    // The entity is built before the outer when(): Mockito rejects stubbing
    // nested inside another stubbing call.
    private void givenPerson() {
        Person person = personEntity();
        when(persons.findById(PERSON_ID)).thenReturn(Optional.of(person));
    }

    private void givenParty(long id, String name, String abbreviation) {
        PoliticalParty party = partyEntity(id, name, abbreviation);
        when(politicalParties.findById(id)).thenReturn(Optional.of(party));
    }

    /** A real entity, so closing a membership is exercised rather than stubbed. */
    private PartyMembership givenCurrentMembership(long partyId, LocalDate startDate) {
        PartyMembership current = new PartyMembership(personEntity(),
                partyEntity(partyId, "Orange Democratic Movement", "ODM"), startDate);
        when(memberships.findCurrentByPerson(PERSON_ID)).thenReturn(Optional.of(current));
        return current;
    }

    private void givenNoCurrentMembership() {
        when(memberships.findCurrentByPerson(PERSON_ID)).thenReturn(Optional.empty());
    }

    // Rejection paths and the id-driven reads never touch some of these
    // stubs; lenient() keeps strict stubs happy.
    private static Person personEntity() {
        Person person = mock(Person.class);
        lenient().when(person.getId()).thenReturn(PERSON_ID);
        lenient().when(person.getFirstName()).thenReturn("Amina");
        lenient().when(person.getLastName()).thenReturn("Odhiambo");
        return person;
    }

    private static PoliticalParty partyEntity(long id, String name, String abbreviation) {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(id);
        lenient().when(party.getName()).thenReturn(name);
        lenient().when(party.getAbbreviation()).thenReturn(abbreviation);
        return party;
    }

    private void savePassesThrough() {
        when(memberships.save(any(PartyMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
