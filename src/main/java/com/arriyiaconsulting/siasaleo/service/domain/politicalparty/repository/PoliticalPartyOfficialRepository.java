package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartyOfficial;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoliticalPartyOfficialRepository
        extends BasicRepository<PoliticalPartyOfficial, Long> {

    // The party's office-holders as they stand, grouped by position.
    @Query("FROM PoliticalPartyOfficial WHERE politicalParty.id = :partyId AND uptoDate IS NULL "
            + "ORDER BY positionName, id")
    List<PoliticalPartyOfficial> findCurrentByParty(@Param("partyId") Long partyId,
                                                    PageRequest pageRequest);

    // The succession, most recent first. NULLS LAST keeps tenures whose start
    // the register never recorded from leading the list.
    @Query("FROM PoliticalPartyOfficial WHERE politicalParty.id = :partyId "
            + "ORDER BY fromDate DESC NULLS LAST, id DESC")
    List<PoliticalPartyOfficial> findByParty(@Param("partyId") Long partyId,
                                             PageRequest pageRequest);

    @Query("FROM PoliticalPartyOfficial WHERE official.id = :personId "
            + "ORDER BY fromDate DESC NULLS LAST, id DESC")
    List<PoliticalPartyOfficial> findByPerson(@Param("personId") Long personId);

    // Guards against appointing the same person to a position they already
    // hold; V19 has no unique index, so this query is the only check there is.
    @Query("FROM PoliticalPartyOfficial WHERE official.id = :personId "
            + "AND politicalParty.id = :partyId AND positionName = :positionName "
            + "AND uptoDate IS NULL")
    Optional<PoliticalPartyOfficial> findCurrentTenure(@Param("personId") Long personId,
                                                       @Param("partyId") Long partyId,
                                                       @Param("positionName") String positionName);
}
