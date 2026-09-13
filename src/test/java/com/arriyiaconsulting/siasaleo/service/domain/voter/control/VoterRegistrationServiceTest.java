package com.arriyiaconsulting.siasaleo.service.domain.voter.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.AreaType;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.ElectoralAreaRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Gender;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.RegisterVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.TransferVoterRequest;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistration;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistrationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.voter.repository.VoterRegistrationRepository;
import jakarta.data.page.PageRequest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the invariants V15 delegates to the application: the centre
 * must be a REGISTRATION_CENTER, a person holds one ACTIVE registration at a
 * time, and retiring a registration keeps it as history instead of
 * overwriting it.
 *
 * Every write is addressed by account, never by person id — the person is the
 * caller's, resolved through PersonProfileService — so these also cover the
 * rule that declaring as a voter is what first creates a person.
 */
@ExtendWith(MockitoExtension.class)
class VoterRegistrationServiceTest {

    private static final long ACCOUNT_ID = 3L;
    private static final long PERSON_ID = 5L;
    private static final long CENTER_ID = 42L;

    private static final ProfileDetails PROFILE =
            new ProfileDetails("Amina", "Odhiambo", LocalDate.of(1994, 2, 8), Gender.FEMALE);

    @Mock
    private VoterRegistrationRepository registrations;

    @Mock
    private PersonProfileService profiles;

    @Mock
    private ElectoralAreaRepository electoralAreas;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private VoterRegistrationService service;

    @Test
    void registersAVoterAsActiveAtTheGivenCentre() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();
        savePassesThrough();

        LocalDate registeredOn = LocalDate.of(2026, 5, 20);
        VoterRegistrationDto created = service.register(ACCOUNT_ID,
                new RegisterVoterRequest(CENTER_ID, PROFILE, registeredOn));

        ArgumentCaptor<VoterRegistration> saved =
                ArgumentCaptor.forClass(VoterRegistration.class);
        verify(registrations).save(saved.capture());
        assertEquals(VoterRegistrationStatus.ACTIVE, saved.getValue().getStatus());
        assertEquals(registeredOn, saved.getValue().getRegistrationDate());
        assertEquals(PERSON_ID, created.personId());
        assertEquals(CENTER_ID, created.registrationCenterId());
        assertEquals("Kibra Primary School", created.registrationCenterName());
        assertEquals("ACTIVE", created.status());
    }

    // Declaring as a voter is what first creates the person, so the profile
    // has to reach PersonProfileService rather than be read here.
    @Test
    void theProfileIsHandedToTheServiceThatOwnsThePerson() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();
        savePassesThrough();

        service.register(ACCOUNT_ID, new RegisterVoterRequest(CENTER_ID, PROFILE, null));

        verify(profiles).resolveFor(ACCOUNT_ID, PROFILE);
    }

    @Test
    void registrationDateDefaultsToToday() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();
        savePassesThrough();

        service.register(ACCOUNT_ID, new RegisterVoterRequest(CENTER_ID, PROFILE, null));

        ArgumentCaptor<VoterRegistration> saved =
                ArgumentCaptor.forClass(VoterRegistration.class);
        verify(registrations).save(saved.capture());
        assertEquals(LocalDate.now(), saved.getValue().getRegistrationDate());
    }

    // The hierarchy runs WORLD..POLLING_STATION; only centres take
    // registrations, stations being the election-day streams below them.
    @ParameterizedTest
    @ValueSource(strings = {"WORLD", "COUNTRY", "COUNTY", "CONSTITUENCY", "WARD",
        "POLLING_STATION"})
    void rejectsAnAreaThatIsNotARegistrationCentre(String areaType) {
        givenCenter(CENTER_ID, "Somewhere", areaType);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(ACCOUNT_ID,
                        new RegisterVoterRequest(CENTER_ID, PROFILE, null)));

        assertTrue(thrown.getMessage().contains("not a REGISTRATION_CENTER"));
        verify(registrations, never()).save(any());
        // The centre is checked first, so a bad one leaves no person behind.
        verify(profiles, never()).resolveFor(anyLong(), any());
    }

    @Test
    void rejectsUnknownCentre() {
        when(electoralAreas.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(ACCOUNT_ID,
                        new RegisterVoterRequest(999L, PROFILE, null)));

        assertTrue(thrown.getMessage().contains("Electoral area not found"));
        verify(registrations, never()).save(any());
        verify(profiles, never()).resolveFor(anyLong(), any());
    }

    // Nothing verifies a self-declared date of birth; this only keeps the
    // obviously ineligible off the roll.
    @Test
    void rejectsAVoterUnderVotingAgeOnTheRegistrationDate() {
        givenPersonBornOn(LocalDate.of(2010, 6, 1));
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(ACCOUNT_ID, new RegisterVoterRequest(
                        CENTER_ID, PROFILE, LocalDate.of(2026, 5, 20))));

        assertTrue(thrown.getMessage().contains("must be 18"));
        verify(registrations, never()).save(any());
    }

    @Test
    void acceptsAVoterWhoTurnsVotingAgeOnTheRegistrationDate() {
        givenPersonBornOn(LocalDate.of(2008, 5, 20));
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();
        savePassesThrough();

        service.register(ACCOUNT_ID, new RegisterVoterRequest(
                CENTER_ID, PROFILE, LocalDate.of(2026, 5, 20)));

        verify(registrations).save(any());
    }

    // A person carried over from another role may have no recorded birth date;
    // the roll does not manufacture an eligibility failure from that.
    @Test
    void allowsRegistrationWhenNoBirthDateWasDeclared() {
        givenPersonBornOn(null);
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();
        savePassesThrough();

        service.register(ACCOUNT_ID, new RegisterVoterRequest(CENTER_ID, PROFILE, null));

        verify(registrations).save(any());
    }

    @Test
    void rejectsASecondRegistrationAndPointsAtTransfer() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenActiveRegistrationAt(CENTER_ID, "Kibra Primary School");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(ACCOUNT_ID,
                        new RegisterVoterRequest(CENTER_ID, PROFILE, null)));

        assertTrue(thrown.getMessage().contains("transfer it instead"));
        verify(registrations, never()).save(any());
    }

    @Test
    void transferRetiresTheOldRegistrationBeforeInsertingTheNewOne() {
        givenPerson();
        givenCenter(7L, "Olympic Primary School", "REGISTRATION_CENTER");
        VoterRegistration current = givenActiveRegistrationAt(CENTER_ID, "Kibra Primary School");
        savePassesThrough();

        VoterRegistrationDto moved = service.transfer(
                ACCOUNT_ID, new TransferVoterRequest(7L, null));

        assertEquals(VoterRegistrationStatus.TRANSFERRED, current.getStatus());
        assertEquals(7L, moved.registrationCenterId());
        assertEquals("ACTIVE", moved.status());

        // The update marking the old row TRANSFERRED has to reach the database
        // before the new ACTIVE row is inserted, or the partial unique index
        // idx_voter_registration_active rejects it.
        ArgumentCaptor<VoterRegistration> saved =
                ArgumentCaptor.forClass(VoterRegistration.class);
        InOrder ordered = inOrder(registrations, entityManager);
        ordered.verify(registrations).save(current);
        ordered.verify(entityManager).flush();
        ordered.verify(registrations).save(saved.capture());
        assertEquals(VoterRegistrationStatus.ACTIVE, saved.getValue().getStatus());
    }

    @Test
    void transferRejectsTheCentreTheVoterIsAlreadyAt() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenActiveRegistrationAt(CENTER_ID, "Kibra Primary School");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(ACCOUNT_ID, new TransferVoterRequest(CENTER_ID, null)));

        assertTrue(thrown.getMessage().contains("already registered at centre"));
        verify(registrations, never()).save(any());
        verify(entityManager, never()).flush();
    }

    @Test
    void transferRequiresAnActiveRegistration() {
        givenPerson();
        givenCenter(7L, "Olympic Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(ACCOUNT_ID, new TransferVoterRequest(7L, null)));

        assertTrue(thrown.getMessage().contains("no active voter registration"));
        verify(registrations, never()).save(any());
    }

    // An account that has never declared any role has no person, so there is
    // nothing to move or withdraw.
    @Test
    void transferRequiresTheCallerToHaveDeclaredARole() {
        when(profiles.findFor(ACCOUNT_ID)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.transfer(ACCOUNT_ID, new TransferVoterRequest(7L, null)));

        assertTrue(thrown.getMessage().contains("not registered as a voter"));
        verify(registrations, never()).save(any());
    }

    @Test
    void deregisterRetiresTheCurrentRegistration() {
        givenPerson();
        VoterRegistration current = givenActiveRegistrationAt(CENTER_ID, "Kibra Primary School");
        savePassesThrough();

        VoterRegistrationDto result = service.deregister(ACCOUNT_ID);

        assertEquals(VoterRegistrationStatus.DEREGISTERED, current.getStatus());
        assertEquals("DEREGISTERED", result.status());
        verify(registrations).save(current);
    }

    @Test
    void deregisterRequiresAnActiveRegistration() {
        givenPerson();
        givenNoActiveRegistration();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.deregister(ACCOUNT_ID));

        assertTrue(thrown.getMessage().contains("no active voter registration"));
        verify(registrations, never()).save(any());
    }

    @Test
    void currentRegistrationIsEmptyOnceDeregistered() {
        givenPerson();
        givenNoActiveRegistration();

        assertTrue(service.findCurrentFor(ACCOUNT_ID).isEmpty());
    }

    @Test
    void anAccountWithNoPersonHasNoRegistrationAndNoHistory() {
        when(profiles.findFor(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertTrue(service.findCurrentFor(ACCOUNT_ID).isEmpty());
        assertTrue(service.findHistoryFor(ACCOUNT_ID).isEmpty());
        verify(registrations, never()).findByPerson(anyLong());
    }

    @Test
    void centreRollListsOnlyActiveRegistrationsAndConvertsPaging() {
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        when(registrations.findByCenterAndStatus(anyLong(), any(VoterRegistrationStatus.class),
                any(PageRequest.class))).thenReturn(List.of());

        service.findByCenter(CENTER_ID, 0, 20);

        ArgumentCaptor<VoterRegistrationStatus> status =
                ArgumentCaptor.forClass(VoterRegistrationStatus.class);
        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(registrations).findByCenterAndStatus(anyLong(), status.capture(),
                pageRequest.capture());
        assertEquals(VoterRegistrationStatus.ACTIVE, status.getValue());
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(1L, pageRequest.getValue().page());
        assertEquals(20, pageRequest.getValue().size());
    }

    @Test
    void centreRollRejectsAnAreaThatIsNotACentre() {
        givenCenter(CENTER_ID, "Kibra", "WARD");

        assertThrows(IllegalArgumentException.class,
                () -> service.findByCenter(CENTER_ID, 0, 20));
        verify(registrations, never()).findByCenterAndStatus(anyLong(),
                any(VoterRegistrationStatus.class), any(PageRequest.class));
    }

    private Person givenPerson() {
        return givenPersonBornOn(LocalDate.of(1994, 2, 8));
    }

    private Person givenPersonBornOn(LocalDate dateOfBirth) {
        Person person = mock(Person.class);
        // Paths that reject on the centre never touch these; lenient() keeps
        // strict stubs happy.
        lenient().when(person.getId()).thenReturn(PERSON_ID);
        lenient().when(person.getFirstName()).thenReturn("Amina");
        lenient().when(person.getLastName()).thenReturn("Odhiambo");
        lenient().when(person.getDateOfBirth()).thenReturn(dateOfBirth);
        lenient().when(profiles.resolveFor(eq(ACCOUNT_ID), any())).thenReturn(person);
        lenient().when(profiles.findFor(ACCOUNT_ID)).thenReturn(Optional.of(person));
        return person;
    }

    private void givenCenter(long id, String name, String typeName) {
        AreaType type = mock(AreaType.class);
        when(type.getName()).thenReturn(typeName);
        ElectoralArea area = mock(ElectoralArea.class);
        // Rejection tests never read these; lenient() keeps strict stubs happy.
        lenient().when(area.getId()).thenReturn(id);
        lenient().when(area.getName()).thenReturn(name);
        when(area.getAreaType()).thenReturn(type);
        when(electoralAreas.findById(id)).thenReturn(Optional.of(area));
    }

    /** A real entity, so status transitions are exercised rather than stubbed. */
    private VoterRegistration givenActiveRegistrationAt(long centerId, String centerName) {
        AreaType type = mock(AreaType.class);
        lenient().when(type.getName()).thenReturn("REGISTRATION_CENTER");
        ElectoralArea center = mock(ElectoralArea.class);
        lenient().when(center.getId()).thenReturn(centerId);
        lenient().when(center.getName()).thenReturn(centerName);
        Person person = mock(Person.class);
        lenient().when(person.getId()).thenReturn(PERSON_ID);
        lenient().when(person.getFirstName()).thenReturn("Amina");
        lenient().when(person.getLastName()).thenReturn("Odhiambo");

        VoterRegistration registration =
                new VoterRegistration(person, center, LocalDate.of(2022, 1, 10));
        when(registrations.findByPersonAndStatus(PERSON_ID, VoterRegistrationStatus.ACTIVE))
                .thenReturn(Optional.of(registration));
        return registration;
    }

    private void givenNoActiveRegistration() {
        when(registrations.findByPersonAndStatus(PERSON_ID, VoterRegistrationStatus.ACTIVE))
                .thenReturn(Optional.empty());
    }

    private void savePassesThrough() {
        lenient().when(registrations.save(any(VoterRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
