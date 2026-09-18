package com.arriyiaconsulting.siasaleo.service.domain.party.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.mapping.PersonMapper;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimDecision;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimResult;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ClaimablePersonDto;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.PersonClaimDto;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaim;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaimStatus;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonClaimRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * The claim path to a person: an account asserts it is somebody the system
 * already holds, and a reviewer decides.
 *
 * This exists because aspirants are usually not new. A sitting officeholder
 * (V14) or a past candidate (V11) already has a person row seeded from public
 * sources, so letting them create another would split one public figure's
 * record across two ids, while linking them to the existing row unchecked
 * would let any account declare itself the Governor. Approval is therefore a
 * human decision, and it is the only thing that writes
 * user_account.person_id on this path.
 *
 * Owns the invariants V40 leaves to the application: the claimant's account is
 * ACTIVE and unlinked, the person is real and unclaimed, and at most one claim
 * on either side is open at a time. The partial unique indexes and the UNIQUE
 * on user_account.person_id remain the real guarantees; these checks turn the
 * common cases into reviewable outcomes.
 */
@ApplicationScoped
public class PersonClaimService {

    @Inject
    private PersonMapper personMapper;

    @Inject
    private PersonClaimRepository claims;

    @Inject
    private PersonRepository persons;

    @Inject
    private UserAccountRepository accounts;

    /**
     * Files a claim. accountId is the authenticated caller, never a value from
     * the request, so nobody can claim a person on another account's behalf.
     */
    @Transactional
    public ClaimResult submit(Long accountId, Long personId, String evidence) {
        UserAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        if (!account.isActive()) {
            return new ClaimResult.AccountUnavailable();
        }
        if (account.getPersonId() != null) {
            return new ClaimResult.AlreadyLinked(account.getPersonId());
        }

        Optional<Person> person = persons.findById(personId);
        if (person.isEmpty()) {
            return new ClaimResult.PersonNotFound(personId);
        }
        if (accounts.findByPersonId(personId).isPresent()) {
            return new ClaimResult.PersonAlreadyClaimed();
        }

        // One open claim per account, then one per person: an account with a
        // claim pending hears about its own before anyone else's.
        Optional<PersonClaim> open = findOpenByAccount(accountId);
        if (open.isEmpty()) {
            open = findOpenByPerson(personId);
        }
        if (open.isPresent()) {
            return new ClaimResult.ClaimAlreadyOpen(personMapper.toPersonClaimDto(open.get()));
        }

        return new ClaimResult.Submitted(personMapper.toPersonClaimDto(
                claims.save(new PersonClaim(accountId, person.get(), evidence))));
    }

    /**
     * Approves a claim and links the account — the two halves are one
     * transaction, so an account is never left approved but unlinked.
     */
    @Transactional
    public ClaimDecision approve(Long claimId, Long reviewerAccountId, String note) {
        Optional<PersonClaim> found = claims.findById(claimId);
        if (found.isEmpty()) {
            return new ClaimDecision.ClaimNotFound(claimId);
        }
        PersonClaim claim = found.get();
        if (!claim.isPending()) {
            return new ClaimDecision.AlreadyDecided(personMapper.toPersonClaimDto(claim));
        }

        Long personId = claim.getPerson().getId();
        // Re-checked at decision time, not just at submission: the person may
        // have been linked in the interval.
        if (accounts.findByPersonId(personId).isPresent()) {
            return new ClaimDecision.PersonAlreadyClaimed();
        }

        UserAccount claimant = accounts.findById(claim.getUserAccountId())
                .orElseThrow(() -> new IllegalStateException(
                        "Claim " + claimId + " references missing account "
                                + claim.getUserAccountId()));
        claimant.linkPerson(personId);
        accounts.save(claimant);

        claim.approve(reviewerAccountId, note);
        return new ClaimDecision.Decided(personMapper.toPersonClaimDto(claims.save(claim)));
    }

    @Transactional
    public ClaimDecision reject(Long claimId, Long reviewerAccountId, String note) {
        return decide(claimId, claim -> claim.reject(reviewerAccountId, note));
    }

    /** The claimant's own retraction; a reviewer uses reject instead. */
    @Transactional
    public ClaimDecision withdraw(Long claimId, Long accountId) {
        Optional<PersonClaim> found = claims.findById(claimId);
        if (found.isPresent() && !found.get().getUserAccountId().equals(accountId)) {
            return new ClaimDecision.NotYours();
        }
        return decide(claimId, PersonClaim::withdraw);
    }

    /** The review queue, oldest first. */
    public List<PersonClaimDto> findPending(int page, int size) {
        return claims.findByStatus(PersonClaimStatus.PENDING, pageRequest(page, size)).stream()
                .map(personMapper::toPersonClaimDto)
                .toList();
    }

    /** Every claim an account has filed, newest first. */
    public List<PersonClaimDto> findByAccount(Long accountId) {
        return claims.findByAccount(accountId).stream()
                .map(personMapper::toPersonClaimDto)
                .toList();
    }

    /**
     * Name search for the claim step, with anyone already linked left out so
     * the claimant is never offered a person they cannot have.
     */
    public List<ClaimablePersonDto> searchClaimable(String name, int page, int size) {
        String pattern = "%" + name.strip().toLowerCase() + "%";
        return persons.searchByName(pattern, pageRequest(page, size)).stream()
                .filter(person -> accounts.findByPersonId(person.getId()).isEmpty())
                .map(personMapper::toClaimablePersonDto)
                .toList();
    }

    private ClaimDecision decide(Long claimId, java.util.function.Consumer<PersonClaim> outcome) {
        Optional<PersonClaim> found = claims.findById(claimId);
        if (found.isEmpty()) {
            return new ClaimDecision.ClaimNotFound(claimId);
        }
        PersonClaim claim = found.get();
        if (!claim.isPending()) {
            return new ClaimDecision.AlreadyDecided(personMapper.toPersonClaimDto(claim));
        }
        outcome.accept(claim);
        return new ClaimDecision.Decided(personMapper.toPersonClaimDto(claims.save(claim)));
    }

    private Optional<PersonClaim> findOpenByAccount(Long accountId) {
        return claims.findByAccountAndStatus(accountId, PersonClaimStatus.PENDING);
    }

    private Optional<PersonClaim> findOpenByPerson(Long personId) {
        return claims.findByPersonAndStatus(personId, PersonClaimStatus.PENDING);
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
