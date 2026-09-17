package com.arriyiaconsulting.siasaleo.service.domain.candidate.control;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.ContestRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.*;
import jakarta.data.page.PageRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidacyServiceTest {
    @Mock CandidacyRepository candidacies;
    @Mock CandidacyStatusRepository statuses;
    @Mock PersonRepository persons;
    @Mock ContestRepository contests;
    @Mock PoliticalPartyRepository politicalParties;
    @Mock VoterRegistrationService voters;
    @Mock PersonProfileService profiles;
    @InjectMocks CandidacyService service;

    private static final long ACCOUNT = 3L;
    private static final ProfileDetails PROFILE = new ProfileDetails("Amina", "Odhiambo",
            LocalDate.of(1994, 2, 8), Gender.FEMALE);
    private static final VoterRegistrationDetails VOTER = new VoterRegistrationDetails(42L, null);

    @Test
    void formForNewAccountIsEditableAndEmptyWithoutCreatingAnything() {
        CandidateRegistrationFormDto form = service.registrationForm(ACCOUNT);
        assertFalse(form.registeredVoter());
        assertFalse(form.voterFieldsReadOnly());
        assertNull(form.profile());
        assertNull(form.voterRegistration());
        verify(profiles).findFor(ACCOUNT);
        verifyNoInteractions(persons, voters, candidacies);
    }

    @Test
    void registeredVoterFormPrefillsAllSharedFieldsAndLocksThem() {
        Person person = person();
        when(profiles.findFor(ACCOUNT)).thenReturn(Optional.of(person));
        VoterRegistrationDto registration = registration();
        when(voters.findCurrent(5L)).thenReturn(Optional.of(registration));
        CandidateRegistrationFormDto form = service.registrationForm(ACCOUNT);
        assertTrue(form.registeredVoter());
        assertTrue(form.voterFieldsReadOnly());
        assertEquals(PROFILE, form.profile());
        assertSame(registration, form.voterRegistration());
        verifyNoInteractions(persons, candidacies);
    }

    @Test
    void existingPersonWithoutActiveVoterRecordHasEditablePrefilledProfile() {
        Person person = person();
        when(profiles.findFor(ACCOUNT)).thenReturn(Optional.of(person));
        CandidateRegistrationFormDto form = service.registrationForm(ACCOUNT);
        assertFalse(form.voterFieldsReadOnly());
        assertEquals(PROFILE, form.profile());
        assertNull(form.voterRegistration());
    }

    @Test
    void registeredVoterCanSubmitOnlyCandidateFields() {
        Person person = ready(null);
        when(voters.findCurrent(5L)).thenReturn(Optional.of(registration()));
        saveCandidacy();
        CandidacyDto result = service.register(ACCOUNT, new RegisterCandidateRequest(null, 7L, null, null));
        assertEquals(5L, result.personId());
        assertEquals("EXPRESSED_INTEREST", result.status());
        assertNull(result.politicalPartyId());
        verify(voters).ensureActiveForCandidate(person, null);
        verify(persons, never()).save(any());
    }

    @Test
    void registeredVoterCannotOverwriteProfileWithSubmittedValues() {
        ProfileDetails changed = new ProfileDetails("Changed", "Name", null, null);
        Person person = ready(changed);
        when(voters.findCurrent(5L)).thenReturn(Optional.of(registration()));
        saveCandidacy();
        service.register(ACCOUNT, new RegisterCandidateRequest(changed, 7L, null, VOTER));
        assertEquals("Amina", person.getFirstName());
        assertEquals(PROFILE.dateOfBirth(), person.getDateOfBirth());
        verify(persons, never()).save(any());
    }

    @Test
    void nonVoterCreatesVoterAndCandidateForTheSameResolvedPerson() {
        Person person = ready(PROFILE);
        when(persons.save(person)).thenReturn(person);
        saveCandidacy();
        service.register(ACCOUNT, new RegisterCandidateRequest(PROFILE, 7L, null, VOTER));
        InOrder order = inOrder(profiles, voters, candidacies);
        order.verify(profiles).resolveFor(ACCOUNT, PROFILE);
        order.verify(voters).findCurrent(5L);
        order.verify(voters).ensureActiveForCandidate(person, VOTER);
        ArgumentCaptor<Candidacy> saved = ArgumentCaptor.forClass(Candidacy.class);
        order.verify(candidacies).save(saved.capture());
        assertSame(person, saved.getValue().getPerson());
    }

    @Test
    void nonVoterCanEditSharedProfileFields() {
        ProfileDetails changed = new ProfileDetails("Updated", "Name", LocalDate.of(1990, 1, 1), Gender.MALE);
        Person person = ready(changed);
        when(persons.save(person)).thenReturn(person);
        saveCandidacy();
        service.register(ACCOUNT, new RegisterCandidateRequest(changed, 7L, null, VOTER));
        assertEquals("Updated", person.getFirstName());
        assertEquals(changed.dateOfBirth(), person.getDateOfBirth());
        verify(voters).ensureActiveForCandidate(person, VOTER);
    }

    @Test
    void nonVoterMissingDetailsCannotSaveCandidacy() {
        Person person = ready(null);
        doThrow(new IllegalArgumentException("Voter details required"))
                .when(voters).ensureActiveForCandidate(person, null);
        assertThrows(IllegalArgumentException.class, () -> service.register(ACCOUNT,
                new RegisterCandidateRequest(null, 7L, null, null)));
        verify(candidacies, never()).save(any());
    }

    @Test
    void duplicateContestIsRejectedBeforeChangingVoterOrProfile() {
        Contest contest = contest();
        when(contests.findById(7L)).thenReturn(Optional.of(contest));
        Person person = person();
        when(profiles.resolveFor(ACCOUNT, null)).thenReturn(person);
        when(candidacies.findByPersonAndContest(5L, 7L)).thenReturn(Optional.of(mock(Candidacy.class)));
        assertThrows(IllegalArgumentException.class, () -> service.register(ACCOUNT,
                new RegisterCandidateRequest(null, 7L, null, null)));
        verifyNoInteractions(voters, persons);
        verify(candidacies, never()).save(any());
    }

    @Test
    void unknownContestOrPartyDoesNotResolveOrCreatePerson() {
        assertThrows(IllegalArgumentException.class, () -> service.register(ACCOUNT,
                new RegisterCandidateRequest(PROFILE, 99L, null, VOTER)));
        when(contests.findById(7L)).thenReturn(Optional.of(mock(Contest.class)));
        assertThrows(IllegalArgumentException.class, () -> service.register(ACCOUNT,
                new RegisterCandidateRequest(PROFILE, 7L, 99L, VOTER)));
        verifyNoInteractions(profiles, voters, persons);
    }

    @Test
    void missingBodyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.register(ACCOUNT, null));
        verifyNoInteractions(profiles, voters, candidacies);
    }

    @Test
    void listByContestConvertsPaging() {
        when(contests.findById(7L)).thenReturn(Optional.of(mock(Contest.class)));
        when(candidacies.findByContest(eq(7L), any())).thenReturn(List.of());
        service.findByContest(7L, 0, 20);
        ArgumentCaptor<PageRequest> paging = ArgumentCaptor.forClass(PageRequest.class);
        verify(candidacies).findByContest(eq(7L), paging.capture());
        assertEquals(1, paging.getValue().page());
        assertEquals(20, paging.getValue().size());
    }

    private Person ready(ProfileDetails details) {
        Contest contest = contest();
        when(contests.findById(7L)).thenReturn(Optional.of(contest));
        Person person = person();
        when(profiles.resolveFor(ACCOUNT, details)).thenReturn(person);
        CandidacyStatus status = mock(CandidacyStatus.class);
        lenient().when(status.getStatusName()).thenReturn("EXPRESSED_INTEREST");
        when(statuses.findByStatusName("EXPRESSED_INTEREST")).thenReturn(Optional.of(status));
        return person;
    }

    private Person person() {
        Person person = spy(new Person(PROFILE.firstName(), PROFILE.lastName(), PROFILE.dateOfBirth(), PROFILE.gender()));
        doReturn(5L).when(person).getId();
        return person;
    }

    private Contest contest() {
        Contest contest = mock(Contest.class);
        when(contest.getId()).thenReturn(7L);
        return contest;
    }

    private VoterRegistrationDto registration() {
        return new VoterRegistrationDto(9L, 5L, "Amina", "Odhiambo", 42L,
                "Kibra Primary School", LocalDate.of(2022, 1, 10), "ACTIVE");
    }

    private void saveCandidacy() {
        when(candidacies.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
