package com.arriyiaconsulting.siasaleo.service.domain.party.repository;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaim;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaimStatus;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonClaimRepository extends BasicRepository<PersonClaim, Long> {

    // Both lookups are only ever called with PENDING, where the V40 partial
    // unique indexes cap the result at one row.
    @Query("FROM PersonClaim WHERE userAccountId = :accountId AND status = :status")
    Optional<PersonClaim> findByAccountAndStatus(@Param("accountId") Long accountId,
                                                 @Param("status") PersonClaimStatus status);

    @Query("FROM PersonClaim WHERE person.id = :personId AND status = :status")
    Optional<PersonClaim> findByPersonAndStatus(@Param("personId") Long personId,
                                                @Param("status") PersonClaimStatus status);

    /** Every claim an account has made, newest first. */
    @Query("FROM PersonClaim WHERE userAccountId = :accountId ORDER BY id DESC")
    List<PersonClaim> findByAccount(@Param("accountId") Long accountId);

    /** The review queue: oldest first, so claims are decided in arrival order. */
    @Query("FROM PersonClaim WHERE status = :status ORDER BY submittedAt")
    List<PersonClaim> findByStatus(@Param("status") PersonClaimStatus status,
                                   PageRequest pageRequest);
}
