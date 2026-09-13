package com.arriyiaconsulting.siasaleo.service.domain.party.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Gender;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the rule that a person appears at the first role declaration
 * and only then: the account must be verified, the first call needs details,
 * and every later call reuses the same person.
 */
@ExtendWith(MockitoExtension.class)
class PersonProfileServiceTest {

    private static final long ACCOUNT_ID = 3L;
    private static final long PERSON_ID = 11L;

    private static final ProfileDetails DETAILS =
            new ProfileDetails("Amina", "Odhiambo", LocalDate.of(1994, 2, 8), Gender.FEMALE);

    @Mock
    private PersonRepository persons;

    @Mock
    private UserAccountRepository accounts;

    @InjectMocks
    private PersonProfileService service;

    @Test
    void createsThePersonAndLinksTheAccountOnTheFirstRoleDeclaration() {
        UserAccount account = givenVerifiedAccount();
        Person saved = givenSaveReturnsPersonWithId();

        Person resolved = service.resolveFor(ACCOUNT_ID, DETAILS);

        assertSame(saved, resolved);
        ArgumentCaptor<Person> created = ArgumentCaptor.forClass(Person.class);
        verify(persons).save(created.capture());
        assertEquals("Amina", created.getValue().getFirstName());
        assertEquals("Odhiambo", created.getValue().getLastName());
        assertEquals(LocalDate.of(1994, 2, 8), created.getValue().getDateOfBirth());
        assertEquals(Gender.FEMALE, created.getValue().getGender());
        assertEquals(PERSON_ID, account.getPersonId());
        verify(accounts).save(account);
    }

    // Nothing verifies a declared gender and nothing guesses one: an omitted
    // value is stored as "not stated" (V41).
    @Test
    void anOmittedGenderIsLeftUnrecorded() {
        givenVerifiedAccount();
        givenSaveReturnsPersonWithId();

        service.resolveFor(ACCOUNT_ID, new ProfileDetails("Amina", "Odhiambo", null, null));

        ArgumentCaptor<Person> created = ArgumentCaptor.forClass(Person.class);
        verify(persons).save(created.capture());
        assertNull(created.getValue().getGender());
    }

    @Test
    void namesAreTrimmedBeforeThePersonIsCreated() {
        givenVerifiedAccount();
        givenSaveReturnsPersonWithId();

        service.resolveFor(ACCOUNT_ID, new ProfileDetails("  Amina ", " Odhiambo  ", null, null));

        ArgumentCaptor<Person> created = ArgumentCaptor.forClass(Person.class);
        verify(persons).save(created.capture());
        assertEquals("Amina", created.getValue().getFirstName());
        assertEquals("Odhiambo", created.getValue().getLastName());
    }

    // The second role — an aspirant after a voter — must land on the same
    // human, or their registration and candidacies split across two ids.
    @Test
    void aSecondRoleReusesTheSamePersonAndIgnoresNewDetails() {
        UserAccount account = givenVerifiedAccount();
        account.linkPerson(PERSON_ID);
        Person existing = mock(Person.class);
        when(persons.findById(PERSON_ID)).thenReturn(Optional.of(existing));

        Person resolved = service.resolveFor(ACCOUNT_ID,
                new ProfileDetails("Someone", "Else", null, Gender.MALE));

        assertSame(existing, resolved);
        verify(persons, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void theFirstCallRequiresDetails() {
        givenVerifiedAccount();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.resolveFor(ACCOUNT_ID, null));

        assertTrue(thrown.getMessage().contains("profile details are required"));
        verify(persons, never()).save(any());
    }

    // Verification proves control of a contact point; until it happens there
    // is nothing to attach a person to.
    @Test
    void anUnverifiedAccountCannotDeclareARole() {
        UserAccount account = new UserAccount(new Identifier.Email("amina@example.com"), "hash");
        when(accounts.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.resolveFor(ACCOUNT_ID, DETAILS));

        assertTrue(thrown.getMessage().contains("PENDING_ACTIVATION"));
        verify(persons, never()).save(any());
    }

    @Test
    void rejectsAnUnknownAccount() {
        when(accounts.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.resolveFor(ACCOUNT_ID, DETAILS));
        verify(persons, never()).save(any());
    }

    @Test
    void personIsAbsentUntilARoleIsDeclared() {
        UserAccount account = givenVerifiedAccount();
        assertTrue(service.findFor(ACCOUNT_ID).isEmpty());
        assertTrue(account.getPersonId() == null);
    }

    private UserAccount givenVerifiedAccount() {
        UserAccount account = new UserAccount(new Identifier.Email("amina@example.com"), "hash");
        account.activate();
        when(accounts.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        return account;
    }

    /** Persisting assigns the id the account is linked to, so it is stubbed here. */
    private Person givenSaveReturnsPersonWithId() {
        Person saved = mock(Person.class);
        when(saved.getId()).thenReturn(PERSON_ID);
        when(persons.save(any(Person.class))).thenReturn(saved);
        return saved;
    }
}
