package com.arriyiaconsulting.siasaleo.service.security.identity.repository;

import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends BasicRepository<UserAccount, Long> {

    // LOWER() in the predicate lets Postgres serve this from
    // idx_user_account_email; the parameter is already normalized lower-case.
    @Query("FROM UserAccount WHERE LOWER(email) = :email")
    Optional<UserAccount> findByEmail(@Param("email") String email);

    @Find
    Optional<UserAccount> findByPhone(@By("phone") String phone);

    // person_id is UNIQUE (V22), so this is the check for "is this person
    // already somebody's account" behind the claim flow.
    @Find
    Optional<UserAccount> findByPersonId(@By("personId") Long personId);

    default Optional<UserAccount> findByIdentifier(Identifier identifier) {
        return switch (identifier) {
            case Identifier.Email(String value) -> findByEmail(value);
            case Identifier.Phone(String value) -> findByPhone(value);
        };
    }
}
