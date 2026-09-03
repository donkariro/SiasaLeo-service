package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AppointOfficialRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyOfficialDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartyOfficial;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyOfficialRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.data.page.PageRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the party-office rules. V19 has no unique index, so these
 * checks are the only guarantee there is: the same person must not be
 * appointed twice to a position they already hold, a tenure must not be closed
 * twice, and a departure date must not precede the day the tenure began —
 * except where the register never recorded that day, which V19 allows.
 */
@ExtendWith(MockitoExtension.class)
class PoliticalPartyOfficialServiceTest {

    private static final long PERSON_ID = 5L;
    private static final long PARTY_ID = 3L;
    private static final long TENURE_ID = 11L;
    private static final String CHAIRPERSON = "Chairperson";

    @Mock
    private PoliticalPartyOfficialRepository officials;

    @Mock
    private PersonRepository persons;

    @Mock
    private PoliticalPartyRepository politicalParties;

    @InjectMocks
    private PoliticalPartyOfficialService service;

    @Test
    void appointRecordsTheTenureAgainstThePartyAndPosition() {
        givenPerson();
        givenParty();
        givenNoCurrentTenure();
        savePassesThrough();

        PoliticalPartyOfficialDto appointed = service.appoint(new AppointOfficialRequest(
                PERSON_ID, PARTY_ID, CHAIRPERSON, null, "official_5.jpeg", "A short note"));

        assertEquals(PERSON_ID, appointed.officialId());
        assertEquals(PARTY_ID, appointed.politicalPartyId());
        assertEquals("Orange Democratic Movement", appointed.partyName());
        assertEquals(CHAIRPERSON, appointed.positionName());
        assertEquals("official_5.jpeg", appointed.photo());
        assertEquals("A short note", appointed.about());
        assertNull(appointed.uptoDate());
        assertTrue(appointed.current());
    }

    @Test
    void appointDefaultsTheStartDateToToday() {
        givenPerson();
        givenParty();
        givenNoCurrentTenure();
        savePassesThrough();

        service.appoint(new AppointOfficialRequest(
                PERSON_ID, PARTY_ID, CHAIRPERSON, null, null, null));

        ArgumentCaptor<PoliticalPartyOfficial> saved =
                ArgumentCaptor.forClass(PoliticalPartyOfficial.class);
        verify(officials).save(saved.capture());
        assertEquals(LocalDate.now(), saved.getValue().getFromDate());
    }

    @Test
    void appointKeepsABackfilledStartDate() {
        givenPerson();
        givenParty();
        givenNoCurrentTenure();
        savePassesThrough();

        LocalDate tookOffice = LocalDate.of(2019, 2, 14);
        PoliticalPartyOfficialDto appointed = service.appoint(new AppointOfficialRequest(
                PERSON_ID, PARTY_ID, CHAIRPERSON, tookOffice, null, null));

        assertEquals(tookOffice, appointed.fromDate());
    }

    // Free-text positions would otherwise let ' Chairperson' read as a
    // different office from 'Chairperson'.
    @Test
    void appointTrimsThePositionBeforeCheckingAndStoringIt() {
        givenPerson();
        givenParty();
        givenNoCurrentTenure();
        savePassesThrough();

        PoliticalPartyOfficialDto appointed = service.appoint(new AppointOfficialRequest(
                PERSON_ID, PARTY_ID, "  Chairperson  ", null, null, null));

        assertEquals(CHAIRPERSON, appointed.positionName());
        verify(officials).findCurrentTenure(PERSON_ID, PARTY_ID, CHAIRPERSON);
    }

    @Test
    void appointRejectsAPositionThePersonAlreadyHolds() {
        givenPerson();
        givenParty();
        // Built before the outer when(): Mockito rejects stubbing nested
        // inside another stubbing call.
        PoliticalPartyOfficial held = tenure(LocalDate.of(2019, 2, 14));
        when(officials.findCurrentTenure(PERSON_ID, PARTY_ID, CHAIRPERSON))
                .thenReturn(Optional.of(held));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.appoint(new AppointOfficialRequest(
                        PERSON_ID, PARTY_ID, CHAIRPERSON, null, null, null)));

        assertTrue(thrown.getMessage().contains("already holds 'Chairperson'"));
        verify(officials, never()).save(any());
    }

    // V19 carries no unique index over (party, position), and the schema
    // comment does not claim one: several people may hold the same position.
    @Test
    void appointAllowsASecondPersonInTheSamePosition() {
        givenPerson();
        givenParty();
        givenNoCurrentTenure();
        savePassesThrough();

        PoliticalPartyOfficialDto appointed = service.appoint(new AppointOfficialRequest(
                PERSON_ID, PARTY_ID, "Deputy Secretary-General", null, null, null));

        assertTrue(appointed.current());
    }

    @Test
    void appointRejectsUnknownPerson() {
        when(persons.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.appoint(new AppointOfficialRequest(
                        999L, PARTY_ID, CHAIRPERSON, null, null, null)));

        assertTrue(thrown.getMessage().contains("Person not found"));
        verify(officials, never()).save(any());
    }

    @Test
    void appointRejectsUnknownParty() {
        givenPerson();
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.appoint(new AppointOfficialRequest(
                        PERSON_ID, 999L, CHAIRPERSON, null, null, null)));

        assertTrue(thrown.getMessage().contains("Political party not found"));
        verify(officials, never()).save(any());
    }

    @Test
    void stepDownClosesTheTenureOnTheGivenDay() {
        PoliticalPartyOfficial current = givenTenure(LocalDate.of(2019, 2, 14));
        savePassesThrough();

        LocalDate leftOn = LocalDate.of(2024, 6, 30);
        PoliticalPartyOfficialDto ended = service.stepDown(TENURE_ID, leftOn);

        assertEquals(leftOn, current.getUptoDate());
        assertEquals(leftOn, ended.uptoDate());
        assertFalse(ended.current());
        verify(officials).save(current);
    }

    @Test
    void stepDownDefaultsToToday() {
        PoliticalPartyOfficial current = givenTenure(LocalDate.now().minusYears(2));
        savePassesThrough();

        service.stepDown(TENURE_ID, null);

        assertEquals(LocalDate.now(), current.getUptoDate());
    }

    // The V19 CHECK allows upto_date = from_date, so a tenure held for a
    // single day is legitimate.
    @Test
    void stepDownAcceptsASingleDayTenure() {
        LocalDate tookOffice = LocalDate.of(2024, 6, 30);
        PoliticalPartyOfficial current = givenTenure(tookOffice);
        savePassesThrough();

        service.stepDown(TENURE_ID, tookOffice);

        assertEquals(tookOffice, current.getUptoDate());
    }

    @Test
    void stepDownRejectsADateBeforeTheTenureBegan() {
        LocalDate tookOffice = LocalDate.of(2024, 6, 30);
        givenTenure(tookOffice);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.stepDown(TENURE_ID, tookOffice.minusDays(1)));

        assertTrue(thrown.getMessage().contains("falls before"));
        verify(officials, never()).save(any());
    }

    // V19 permits an end date with no start, for records whose beginning the
    // register never captured; there is then nothing to compare against.
    @Test
    void stepDownAcceptsAnyDateWhenTheTenureHasNoRecordedStart() {
        PoliticalPartyOfficial current = givenTenure(null);
        savePassesThrough();

        LocalDate leftOn = LocalDate.of(2015, 1, 1);
        service.stepDown(TENURE_ID, leftOn);

        assertEquals(leftOn, current.getUptoDate());
    }

    @Test
    void stepDownRejectsATenureThatAlreadyEnded() {
        PoliticalPartyOfficial closed = tenure(LocalDate.of(2019, 2, 14));
        closed.end(LocalDate.of(2024, 6, 30));
        when(officials.findById(TENURE_ID)).thenReturn(Optional.of(closed));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.stepDown(TENURE_ID, null));

        assertTrue(thrown.getMessage().contains("already ended on 2024-06-30"));
        verify(officials, never()).save(any());
    }

    @Test
    void stepDownRejectsAnUnknownTenure() {
        when(officials.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.stepDown(999L, null));

        assertTrue(thrown.getMessage().contains("Party office tenure not found"));
        verify(officials, never()).save(any());
    }

    @Test
    void listingAPartyDefaultsToTheOfficialsInOffice() {
        givenParty();
        when(officials.findCurrentByParty(anyLong(), any(PageRequest.class)))
                .thenReturn(List.of());

        service.findByParty(PARTY_ID, true, 0, 20);

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(officials).findCurrentByParty(anyLong(), pageRequest.capture());
        verify(officials, never()).findByParty(anyLong(), any(PageRequest.class));
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(1L, pageRequest.getValue().page());
        assertEquals(20, pageRequest.getValue().size());
    }

    @Test
    void listingAPartyWidensToTheSuccessionWhenCurrentIsOff() {
        givenParty();
        when(officials.findByParty(anyLong(), any(PageRequest.class))).thenReturn(List.of());

        service.findByParty(PARTY_ID, false, 0, 20);

        verify(officials).findByParty(anyLong(), any(PageRequest.class));
        verify(officials, never()).findCurrentByParty(anyLong(), any(PageRequest.class));
    }

    @Test
    void listingRejectsAnUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.findByParty(999L, true, 0, 20));
        verify(officials, never()).findCurrentByParty(anyLong(), any(PageRequest.class));
    }

    @Test
    void aPersonsOfficesIncludeTheTenuresTheyHaveLeft() {
        givenPerson();
        PoliticalPartyOfficial closed = tenure(LocalDate.of(2013, 1, 10));
        closed.end(LocalDate.of(2019, 2, 13));
        PoliticalPartyOfficial open = tenure(LocalDate.of(2019, 2, 14));
        when(officials.findByPerson(PERSON_ID)).thenReturn(List.of(open, closed));

        List<PoliticalPartyOfficialDto> held = service.findByPerson(PERSON_ID);

        assertEquals(2, held.size());
        assertTrue(held.get(0).current());
        assertFalse(held.get(1).current());
        assertEquals(LocalDate.of(2019, 2, 13), held.get(1).uptoDate());
    }

    @Test
    void aPersonsOfficesRejectAnUnknownPerson() {
        when(persons.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findByPerson(999L));
        verify(officials, never()).findByPerson(anyLong());
    }

    private void givenPerson() {
        Person person = personEntity();
        when(persons.findById(PERSON_ID)).thenReturn(Optional.of(person));
    }

    private void givenParty() {
        PoliticalParty party = partyEntity();
        when(politicalParties.findById(PARTY_ID)).thenReturn(Optional.of(party));
    }

    private void givenNoCurrentTenure() {
        when(officials.findCurrentTenure(anyLong(), anyLong(), anyString()))
                .thenReturn(Optional.empty());
    }

    /** A real entity, so closing a tenure is exercised rather than stubbed. */
    private PoliticalPartyOfficial givenTenure(LocalDate fromDate) {
        PoliticalPartyOfficial current = tenure(fromDate);
        when(officials.findById(TENURE_ID)).thenReturn(Optional.of(current));
        return current;
    }

    private static PoliticalPartyOfficial tenure(LocalDate fromDate) {
        return new PoliticalPartyOfficial(personEntity(), partyEntity(),
                CHAIRPERSON, fromDate, null, null);
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

    private static PoliticalParty partyEntity() {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(PARTY_ID);
        lenient().when(party.getName()).thenReturn("Orange Democratic Movement");
        lenient().when(party.getAbbreviation()).thenReturn("ODM");
        return party;
    }

    private void savePassesThrough() {
        when(officials.save(any(PoliticalPartyOfficial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
