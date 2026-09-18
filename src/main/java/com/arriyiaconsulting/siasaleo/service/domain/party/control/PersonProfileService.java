package com.arriyiaconsulting.siasaleo.service.domain.party.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.mapping.PersonMapper;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;

/**
 * The single place an account acquires the person it represents.
 *
 * A person row is deliberately not created at sign-up or at verification.
 * Verification proves control of an email or phone and says nothing about a
 * human; creating a row then would mean either collecting names in
 * RegisterRequest — which the sign-up flow purposely keeps to a contact point
 * and a password — or inserting a placeholder into the same table that holds
 * real officeholders and candidates, where it would surface in every name
 * search. Nor does every account need one: someone only reading election data
 * is not a party-model entity.
 *
 * So the person appears at the first role declaration, which is also the first
 * moment the attributes the role needs are worth asking for. Every role goes
 * through resolveFor, and it is idempotent: the user who declares as a voter
 * and later acquires another role keeps one person throughout.
 *
 * This is the *create* path, for roles nobody would impersonate. An aspirant
 * usually already exists here as a sitting officeholder or past candidate, so
 * that path claims an existing row through PersonClaimService instead — and
 * that claim is reviewed, since creating a second row for a public figure
 * would split their history, and linking to one unchecked would hand an
 * account authority over it.
 */
@ApplicationScoped
public class PersonProfileService {

    @Inject
    private PersonMapper personMapper;

    @Inject
    private PersonRepository persons;

    @Inject
    private UserAccountRepository accounts;

    /**
     * The person this account represents, creating it from details on first
     * call. Callers hold the surrounding transaction, so a role declaration
     * that fails afterwards leaves no orphan person behind.
     *
     * @param details required only on the first call; ignored once linked, so
     *                a later role cannot quietly rewrite the user's name.
     */
    @Transactional
    public Person resolveFor(Long accountId, ProfileDetails details) {
        UserAccount account = requireActiveAccount(accountId);
        if (account.getPersonId() != null) {
            return requirePerson(account.getPersonId());
        }
        if (details == null) {
            throw new IllegalArgumentException(
                    "Account " + accountId + " has no person yet; profile details are required");
        }

        Person person = persons.save(personMapper.toEntity(details));
        account.linkPerson(person.getId());
        accounts.save(account);
        return person;
    }

    /** The person this account represents, absent until it declares a role. */
    public Optional<Person> findFor(Long accountId) {
        return accounts.findById(accountId)
                .map(UserAccount::getPersonId)
                .flatMap(persons::findById);
    }

    private UserAccount requireActiveAccount(Long accountId) {
        UserAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        if (!account.isActive()) {
            throw new IllegalArgumentException("Account " + accountId
                    + " is " + account.getStatus() + "; verify it before declaring a role");
        }
        return account;
    }

    // A dangling person_id means the party row was deleted under a live
    // account — a data fault, not a user error, so it fails loudly.
    private Person requirePerson(Long personId) {
        return persons.findById(personId)
                .orElseThrow(() -> new IllegalStateException(
                        "Account references missing person " + personId));
    }
}
