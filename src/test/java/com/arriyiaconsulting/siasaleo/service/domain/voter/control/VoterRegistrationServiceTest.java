package com.arriyiaconsulting.siasaleo.service.domain.voter.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.AreaType;
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
 */
@ExtendWith(MockitoExtension.class)
class VoterRegistrationServiceTest {

    private static final long PERSON_ID = 5L;
    private static final long CENTER_ID = 42L;

    @Mock
    private VoterRegistrationRepository registrations;

    @Mock
    private PersonRepository persons;

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
        VoterRegistrationDto created = service.register(
                new RegisterVoterRequest(PERSON_ID, CENTER_ID, registeredOn));

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

    @Test
    void registrationDateDefaultsToToday() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenNoActiveRegistration();
        savePassesThrough();

        service.register(new RegisterVoterRequest(PERSON_ID, CENTER_ID, null));

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
        givenPerson();
        givenCenter(CENTER_ID, "Somewhere", areaType);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterVoterRequest(PERSON_ID, CENTER_ID, null)));

        assertTrue(thrown.getMessage().contains("not a REGISTRATION_CENTER"));
        verify(registrations, never()).save(any());
    }

    @Test
    void rejectsUnknownPerson() {
        when(persons.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterVoterRequest(999L, CENTER_ID, null)));

        assertTrue(thrown.getMessage().contains("Person not found"));
        verify(registrations, never()).save(any());
    }

    @Test
    void rejectsUnknownCentre() {
        givenPerson();
        when(electoralAreas.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterVoterRequest(PERSON_ID, 999L, null)));

        assertTrue(thrown.getMessage().contains("Electoral area not found"));
        verify(registrations, never()).save(any());
    }

    @Test
    void rejectsASecondRegistrationAndPointsAtTransfer() {
        givenPerson();
        givenCenter(CENTER_ID, "Kibra Primary School", "REGISTRATION_CENTER");
        givenActiveRegistrationAt(CENTER_ID, "Kibra Primary School");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterVoterRequest(PERSON_ID, CENTER_ID, null)));

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
                PERSON_ID, new TransferVoterRequest(7L, null));

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
                () -> service.transfer(PERSON_ID, new TransferVoterRequest(CENTER_ID, null)));

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
                () -> service.transfer(PERSON_ID, new TransferVoterRequest(7L, null)));

        assertTrue(thrown.getMessage().contains("no active voter registration"));
        verify(registrations, never()).save(any());
    }

    @Test
    void deregisterRetiresTheCurrentRegistration() {
        givenPerson();
        VoterRegistration current = givenActiveRegistrationAt(CENTER_ID, "Kibra Primary School");
        savePassesThrough();

        VoterRegistrationDto result = service.deregister(PERSON_ID);

        assertEquals(VoterRegistrationStatus.DEREGISTERED, current.getStatus());
        assertEquals("DEREGISTERED", result.status());
        verify(registrations).save(current);
    }

    @Test
    void deregisterRequiresAnActiveRegistration() {
        givenPerson();
        givenNoActiveRegistration();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.deregister(PERSON_ID));

        assertTrue(thrown.getMessage().contains("no active voter registration"));
        verify(registrations, never()).save(any());
    }

    @Test
    void currentRegistrationIsEmptyOnceDeregistered() {
        givenPerson();
        givenNoActiveRegistration();

        assertTrue(service.findCurrent(PERSON_ID).isEmpty());
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

    private void givenPerson() {
        Person person = mock(Person.class);
        // Paths that reject on the centre, and the person-scoped reads that
        // work off the id parameter, never touch these; lenient() keeps
        // strict stubs happy.
        lenient().when(person.getId()).thenReturn(PERSON_ID);
        lenient().when(person.getFirstName()).thenReturn("Amina");
        lenient().when(person.getLastName()).thenReturn("Odhiambo");
        when(persons.findById(PERSON_ID)).thenReturn(Optional.of(person));
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
        lenient().when(center.getAreaType()).thenReturn(type);
        Person person = mock(Person.class);
        lenient().when(person.getId()).thenReturn(PERSON_ID);
        lenient().when(person.getFirstName()).thenReturn("Amina");
        lenient().when(person.getLastName()).thenReturn("Odhiambo");

        VoterRegistration active = new VoterRegistration(person, center, LocalDate.now());
        when(registrations.findByPersonAndStatus(PERSON_ID, VoterRegistrationStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        return active;
    }

    private void givenNoActiveRegistration() {
        when(registrations.findByPersonAndStatus(PERSON_ID, VoterRegistrationStatus.ACTIVE))
                .thenReturn(Optional.empty());
    }

    private void savePassesThrough() {
        when(registrations.save(any(VoterRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
