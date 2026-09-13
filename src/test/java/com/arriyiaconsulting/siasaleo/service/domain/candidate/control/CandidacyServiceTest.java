package com.arriyiaconsulting.siasaleo.service.domain.candidate.control;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.CandidacyDto;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.RegisterCandidateRequest;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.RegisterCandidateRequest.NewPerson;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Gender;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the candidate-registration rules: exactly one person
 * reference per request, all referenced rows must exist, one candidacy per
 * person per contest, and every new candidacy starts at the first stage of
 * the V21 lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class CandidacyServiceTest {

    @Mock
    private CandidacyRepository candidacies;

    @Mock
    private CandidacyStatusRepository statuses;

    @Mock
    private PersonRepository persons;

    @Mock
    private ContestRepository contests;

    @Mock
    private PoliticalPartyRepository politicalParties;

    @InjectMocks
    private CandidacyService service;

    @Test
    void registersExistingPersonAtTheInitialLifecycleStage() {
        givenContest(7L);
        givenParty(3L, "Orange Democratic Movement", "ODM");
        givenPerson(5L, "Amina", "Odhiambo");
        when(candidacies.findByPersonAndContest(5L, 7L)).thenReturn(Optional.empty());
        givenInitialStatus();
        savePassesThrough();

        CandidacyDto created = service.register(
                new RegisterCandidateRequest(5L, null, 7L, 3L));

        ArgumentCaptor<Candidacy> saved = ArgumentCaptor.forClass(Candidacy.class);
        verify(candidacies).save(saved.capture());
        assertEquals(CandidacyService.INITIAL_STATUS,
                saved.getValue().getStatus().getStatusName());
        assertEquals(5L, created.personId());
        assertEquals("Amina", created.firstName());
        assertEquals("Odhiambo", created.lastName());
        assertEquals(7L, created.contestId());
        assertEquals(3L, created.politicalPartyId());
        assertEquals("Orange Democratic Movement", created.partyName());
        assertEquals("ODM", created.partyAbbreviation());
        assertEquals(CandidacyService.INITIAL_STATUS, created.status());
    }

    @Test
    void createsThePersonWhenDetailsAreGiven() {
        givenContest(7L);
        givenInitialStatus();
        when(persons.save(any(Person.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        savePassesThrough();

        LocalDate dob = LocalDate.of(1980, 3, 14);
        service.register(new RegisterCandidateRequest(
                null, new NewPerson("Wanjiku", "Kamau", dob, Gender.FEMALE), 7L, null));

        ArgumentCaptor<Person> person = ArgumentCaptor.forClass(Person.class);
        verify(persons).save(person.capture());
        assertEquals("Wanjiku", person.getValue().getFirstName());
        assertEquals("Kamau", person.getValue().getLastName());
        assertEquals(dob, person.getValue().getDateOfBirth());
        assertEquals(Gender.FEMALE, person.getValue().getGender());
        ArgumentCaptor<Candidacy> saved = ArgumentCaptor.forClass(Candidacy.class);
        verify(candidacies).save(saved.capture());
        assertEquals(person.getValue(), saved.getValue().getPerson());
        // A person created in this request cannot already be in the contest.
        verify(candidacies, never()).findByPersonAndContest(anyLong(), anyLong());
    }

    @Test
    void registersIndependentWhenNoPartyIsGiven() {
        givenContest(7L);
        givenPerson(5L, "Amina", "Odhiambo");
        when(candidacies.findByPersonAndContest(5L, 7L)).thenReturn(Optional.empty());
        givenInitialStatus();
        savePassesThrough();

        CandidacyDto created = service.register(
                new RegisterCandidateRequest(5L, null, 7L, null));

        assertNull(created.politicalPartyId());
        assertNull(created.partyName());
        assertNull(created.partyAbbreviation());
        verify(politicalParties, never()).findById(anyLong());
    }

    @Test
    void rejectsBothPersonIdAndPersonDetails() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterCandidateRequest(
                        5L, new NewPerson("Amina", "Odhiambo", null, null), 7L, null)));

        assertTrue(thrown.getMessage().contains("exactly one"));
        verify(candidacies, never()).save(any());
    }

    @Test
    void rejectsMissingPersonReference() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterCandidateRequest(null, null, 7L, null)));

        assertTrue(thrown.getMessage().contains("exactly one"));
        verify(candidacies, never()).save(any());
    }

    @Test
    void rejectsUnknownContest() {
        when(contests.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterCandidateRequest(5L, null, 999L, null)));

        assertTrue(thrown.getMessage().contains("Contest not found"));
        verify(candidacies, never()).save(any());
    }

    @Test
    void rejectsUnknownPoliticalParty() {
        givenContest(7L);
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterCandidateRequest(5L, null, 7L, 999L)));

        assertTrue(thrown.getMessage().contains("Political party not found"));
        verify(candidacies, never()).save(any());
    }

    @Test
    void rejectsUnknownPerson() {
        givenContest(7L);
        when(persons.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterCandidateRequest(999L, null, 7L, null)));

        assertTrue(thrown.getMessage().contains("Person not found"));
        verify(candidacies, never()).save(any());
    }

    @Test
    void rejectsARepeatRegistrationInTheSameContest() {
        givenContest(7L);
        givenPerson(5L, "Amina", "Odhiambo");
        when(candidacies.findByPersonAndContest(5L, 7L))
                .thenReturn(Optional.of(mock(Candidacy.class)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterCandidateRequest(5L, null, 7L, null)));

        assertTrue(thrown.getMessage().contains("already registered"));
        verify(candidacies, never()).save(any());
    }

    @Test
    void listByContestConvertsPaging() {
        givenContest(7L);
        when(candidacies.findByContest(anyLong(), any(PageRequest.class)))
                .thenReturn(List.of());

        service.findByContest(7L, 0, 20);

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(candidacies).findByContest(anyLong(), pageRequest.capture());
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(1L, pageRequest.getValue().page());
        assertEquals(20, pageRequest.getValue().size());
    }

    @Test
    void listByUnknownContestQueriesNothing() {
        when(contests.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.findByContest(999L, 0, 20));
        verify(candidacies, never()).findByContest(anyLong(), any(PageRequest.class));
    }

    private void givenContest(long id) {
        Contest contest = mock(Contest.class);
        // Rejection tests fail before the id is read; lenient() keeps strict
        // stubs happy.
        lenient().when(contest.getId()).thenReturn(id);
        when(contests.findById(id)).thenReturn(Optional.of(contest));
    }

    private void givenParty(long id, String name, String abbreviation) {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(id);
        lenient().when(party.getName()).thenReturn(name);
        lenient().when(party.getAbbreviation()).thenReturn(abbreviation);
        when(politicalParties.findById(id)).thenReturn(Optional.of(party));
    }

    private void givenPerson(long id, String firstName, String lastName) {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        lenient().when(person.getFirstName()).thenReturn(firstName);
        lenient().when(person.getLastName()).thenReturn(lastName);
        when(persons.findById(id)).thenReturn(Optional.of(person));
    }

    private void givenInitialStatus() {
        CandidacyStatus status = mock(CandidacyStatus.class);
        lenient().when(status.getStatusName()).thenReturn(CandidacyService.INITIAL_STATUS);
        when(statuses.findByStatusName(CandidacyService.INITIAL_STATUS))
                .thenReturn(Optional.of(status));
    }

    private void savePassesThrough() {
        when(candidacies.save(any(Candidacy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
