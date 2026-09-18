package com.arriyiaconsulting.siasaleo.service.domain.party.control;

import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import com.arriyiaconsulting.siasaleo.service.domain.party.mapping.PersonMapper;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimDecision;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimResult;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaim;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaimStatus;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonClaimRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import jakarta.data.page.PageRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the invariants V40 delegates to the application: only a
 * verified, unlinked account may claim; only an unclaimed person may be
 * claimed; one claim is open per account and per person; and approval — the
 * only step that links the account — re-checks the person in case it was
 * taken while the claim sat in the queue.
 */
@ExtendWith(MockitoExtension.class)
class PersonClaimServiceTest {

    @Spy
    private PersonMapper personMapper = Mappers.getMapper(PersonMapper.class);


    private static final long ACCOUNT_ID = 3L;
    private static final long REVIEWER_ID = 1L;
    private static final long PERSON_ID = 11L;
    private static final long CLAIM_ID = 77L;

    @Mock
    private PersonClaimRepository claims;

    @Mock
    private PersonRepository persons;

    @Mock
    private UserAccountRepository accounts;

    @InjectMocks
    private PersonClaimService service;

    @Test
    void submitFilesAPendingClaimOnTheNamedPerson() {
        givenVerifiedUnlinkedAccount();
        Person person = givenPerson();
        givenPersonUnclaimed();
        givenNoOpenClaims();
        savePassesThrough();

        ClaimResult result = service.submit(ACCOUNT_ID, PERSON_ID, "Gazette notice 1234");

        ArgumentCaptor<PersonClaim> saved = ArgumentCaptor.forClass(PersonClaim.class);
        verify(claims).save(saved.capture());
        assertEquals(PersonClaimStatus.PENDING, saved.getValue().getStatus());
        assertEquals(ACCOUNT_ID, saved.getValue().getUserAccountId());
        assertEquals(person, saved.getValue().getPerson());
        assertEquals("Gazette notice 1234", saved.getValue().getEvidence());
        assertEquals("PENDING", assertInstanceOf(ClaimResult.Submitted.class, result)
                .claim().status());
    }

    @Test
    void submitRefusesAnUnverifiedAccount() {
        UserAccount account = new UserAccount(new Identifier.Email("a@example.com"), "hash");
        when(accounts.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        assertInstanceOf(ClaimResult.AccountUnavailable.class,
                service.submit(ACCOUNT_ID, PERSON_ID, null));
        verify(claims, never()).save(any());
    }

    // An account that already resolves to a person has nobody left to claim,
    // and user_account.person_id is UNIQUE besides.
    @Test
    void submitRefusesAnAccountThatIsAlreadyLinked() {
        UserAccount account = givenVerifiedUnlinkedAccount();
        account.linkPerson(99L);

        ClaimResult result = service.submit(ACCOUNT_ID, PERSON_ID, null);

        assertEquals(99L, assertInstanceOf(ClaimResult.AlreadyLinked.class, result).personId());
        verify(claims, never()).save(any());
    }

    @Test
    void submitRefusesAnUnknownPerson() {
        givenVerifiedUnlinkedAccount();
        when(persons.findById(PERSON_ID)).thenReturn(Optional.empty());

        assertInstanceOf(ClaimResult.PersonNotFound.class,
                service.submit(ACCOUNT_ID, PERSON_ID, null));
        verify(claims, never()).save(any());
    }

    @Test
    void submitRefusesAPersonAnotherAccountAlreadyHolds() {
        givenVerifiedUnlinkedAccount();
        givenPerson();
        when(accounts.findByPersonId(PERSON_ID))
                .thenReturn(Optional.of(mock(UserAccount.class)));

        assertInstanceOf(ClaimResult.PersonAlreadyClaimed.class,
                service.submit(ACCOUNT_ID, PERSON_ID, null));
        verify(claims, never()).save(any());
    }

    @Test
    void submitRefusesASecondOpenClaimAndReturnsTheStandingOne() {
        givenVerifiedUnlinkedAccount();
        givenPerson();
        givenPersonUnclaimed();
        PersonClaim open = new PersonClaim(ACCOUNT_ID, givenPerson(), "earlier");
        when(claims.findByAccountAndStatus(ACCOUNT_ID, PersonClaimStatus.PENDING))
                .thenReturn(Optional.of(open));

        ClaimResult result = service.submit(ACCOUNT_ID, PERSON_ID, "again");

        assertEquals("PENDING",
                assertInstanceOf(ClaimResult.ClaimAlreadyOpen.class, result).existing().status());
        verify(claims, never()).save(any());
    }

    // Two people racing for the same politician: the second is turned away
    // before the partial unique index has to.
    @Test
    void submitRefusesAPersonSomeoneElseHasAClaimOpenOn() {
        givenVerifiedUnlinkedAccount();
        givenPerson();
        givenPersonUnclaimed();
        when(claims.findByAccountAndStatus(ACCOUNT_ID, PersonClaimStatus.PENDING))
                .thenReturn(Optional.empty());
        PersonClaim theirs = new PersonClaim(999L, givenPerson(), "theirs");
        when(claims.findByPersonAndStatus(PERSON_ID, PersonClaimStatus.PENDING))
                .thenReturn(Optional.of(theirs));

        assertInstanceOf(ClaimResult.ClaimAlreadyOpen.class,
                service.submit(ACCOUNT_ID, PERSON_ID, null));
        verify(claims, never()).save(any());
    }

    @Test
    void approveLinksTheAccountAndSettlesTheClaimTogether() {
        UserAccount claimant = givenVerifiedUnlinkedAccount();
        PersonClaim claim = givenPendingClaim();
        givenPersonUnclaimed();
        savePassesThrough();

        ClaimDecision decision = service.approve(CLAIM_ID, REVIEWER_ID, "Verified by phone");

        assertEquals(PERSON_ID, claimant.getPersonId());
        verify(accounts).save(claimant);
        assertEquals(PersonClaimStatus.APPROVED, claim.getStatus());
        assertEquals(REVIEWER_ID, claim.getDecidedBy());
        assertEquals("Verified by phone",
                assertInstanceOf(ClaimDecision.Decided.class, decision).claim().decisionNote());
    }

    // The person may have been linked while the claim sat in the queue.
    @Test
    void approveRechecksThatThePersonIsStillFree() {
        givenPendingClaim();
        when(accounts.findByPersonId(PERSON_ID)).thenReturn(Optional.of(mock(UserAccount.class)));

        assertInstanceOf(ClaimDecision.PersonAlreadyClaimed.class,
                service.approve(CLAIM_ID, REVIEWER_ID, null));
        verify(claims, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void aDecidedClaimIsNotDecidedAgain() {
        PersonClaim claim = givenPendingClaim();
        claim.reject(REVIEWER_ID, "Could not verify");

        ClaimDecision decision = service.approve(CLAIM_ID, REVIEWER_ID, "changed my mind");

        assertEquals("REJECTED",
                assertInstanceOf(ClaimDecision.AlreadyDecided.class, decision).claim().status());
        verify(accounts, never()).save(any());
    }

    @Test
    void approveReportsAnUnknownClaim() {
        when(claims.findById(CLAIM_ID)).thenReturn(Optional.empty());

        assertEquals(CLAIM_ID, assertInstanceOf(ClaimDecision.ClaimNotFound.class,
                service.approve(CLAIM_ID, REVIEWER_ID, null)).claimId());
    }

    @Test
    void rejectSettlesTheClaimWithoutLinkingAnything() {
        UserAccount claimant = givenVerifiedUnlinkedAccount();
        PersonClaim claim = givenPendingClaim();
        savePassesThrough();

        service.reject(CLAIM_ID, REVIEWER_ID, "Could not verify");

        assertEquals(PersonClaimStatus.REJECTED, claim.getStatus());
        assertNull(claimant.getPersonId());
        verify(accounts, never()).save(any());
    }

    @Test
    void withdrawIsOnlyForTheClaimant() {
        givenPendingClaim();

        assertInstanceOf(ClaimDecision.NotYours.class, service.withdraw(CLAIM_ID, 999L));
        verify(claims, never()).save(any());
    }

    @Test
    void theClaimantMayWithdrawAndNoReviewerIsRecorded() {
        PersonClaim claim = givenPendingClaim();
        savePassesThrough();

        service.withdraw(CLAIM_ID, ACCOUNT_ID);

        assertEquals(PersonClaimStatus.WITHDRAWN, claim.getStatus());
        assertNull(claim.getDecidedBy());
    }

    // Offering a person who cannot be claimed sends the user down a dead end.
    @Test
    void claimableSearchLeavesOutPeopleAnAccountAlreadyHolds() {
        Person free = givenPersonWithId(11L);
        Person taken = givenPersonWithId(12L);
        when(persons.searchByName(any(), any(PageRequest.class)))
                .thenReturn(List.of(free, taken));
        when(accounts.findByPersonId(11L)).thenReturn(Optional.empty());
        when(accounts.findByPersonId(12L)).thenReturn(Optional.of(mock(UserAccount.class)));

        assertEquals(List.of(personMapper.toClaimablePersonDto(free)),
                service.searchClaimable("  Odhiambo ", 0, 20));

        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        verify(persons).searchByName(pattern.capture(), any(PageRequest.class));
        assertEquals("%odhiambo%", pattern.getValue());
    }

    @Test
    void theReviewQueueAsksForPendingClaimsAndConvertsPaging() {
        when(claims.findByStatus(any(PersonClaimStatus.class), any(PageRequest.class)))
                .thenReturn(List.of());

        service.findPending(0, 50);

        ArgumentCaptor<PersonClaimStatus> status =
                ArgumentCaptor.forClass(PersonClaimStatus.class);
        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(claims).findByStatus(status.capture(), pageRequest.capture());
        assertEquals(PersonClaimStatus.PENDING, status.getValue());
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(1L, pageRequest.getValue().page());
        assertEquals(50, pageRequest.getValue().size());
    }

    private UserAccount givenVerifiedUnlinkedAccount() {
        UserAccount account = new UserAccount(new Identifier.Email("amina@example.com"), "hash");
        account.activate();
        lenient().when(accounts.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        return account;
    }

    private Person givenPerson() {
        Person person = givenPersonWithId(PERSON_ID);
        lenient().when(persons.findById(PERSON_ID)).thenReturn(Optional.of(person));
        return person;
    }

    private Person givenPersonWithId(long id) {
        Person person = mock(Person.class);
        lenient().when(person.getId()).thenReturn(id);
        lenient().when(person.getFirstName()).thenReturn("Amina");
        lenient().when(person.getLastName()).thenReturn("Odhiambo");
        return person;
    }

    private void givenPersonUnclaimed() {
        lenient().when(accounts.findByPersonId(PERSON_ID)).thenReturn(Optional.empty());
    }

    private void givenNoOpenClaims() {
        lenient().when(claims.findByAccountAndStatus(anyLong(), any(PersonClaimStatus.class)))
                .thenReturn(Optional.empty());
        lenient().when(claims.findByPersonAndStatus(anyLong(), any(PersonClaimStatus.class)))
                .thenReturn(Optional.empty());
    }

    /** A real entity, so the status transitions are exercised rather than stubbed. */
    private PersonClaim givenPendingClaim() {
        PersonClaim claim = new PersonClaim(ACCOUNT_ID, givenPerson(), "Gazette notice 1234");
        lenient().when(claims.findById(CLAIM_ID)).thenReturn(Optional.of(claim));
        return claim;
    }

    private void savePassesThrough() {
        lenient().when(claims.save(any(PersonClaim.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
