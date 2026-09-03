package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PartyMembership;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyMembershipRepository extends BasicRepository<PartyMembership, Long> {

    // idx_party_membership_current caps this at one row per person.
    @Query("FROM PartyMembership WHERE person.id = :personId AND endDate IS NULL")
    Optional<PartyMembership> findCurrentByPerson(@Param("personId") Long personId);

    // Most recent first, so the current membership leads. Ordered by start
    // date rather than by id: imported history need not arrive in date order.
    @Query("FROM PartyMembership WHERE person.id = :personId ORDER BY startDate DESC, id DESC")
    List<PartyMembership> findByPerson(@Param("personId") Long personId);

    @Query("FROM PartyMembership WHERE politicalParty.id = :partyId AND endDate IS NULL ORDER BY id")
    List<PartyMembership> findCurrentByParty(@Param("partyId") Long partyId,
                                             PageRequest pageRequest);
}
