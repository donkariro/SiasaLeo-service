package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySlogan;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface PoliticalPartySloganRepository
        extends BasicRepository<PoliticalPartySlogan, Long> {

    // Several may be open at once: V20 puts no unique index over slogans.
    @Query("FROM PoliticalPartySlogan WHERE politicalParty.id = :partyId AND uptoDate IS NULL "
            + "ORDER BY fromDate DESC NULLS LAST, id DESC")
    List<PoliticalPartySlogan> findCurrentByParty(@Param("partyId") Long partyId);

    @Query("FROM PoliticalPartySlogan WHERE politicalParty.id = :partyId "
            + "ORDER BY fromDate DESC NULLS LAST, id DESC")
    List<PoliticalPartySlogan> findByParty(@Param("partyId") Long partyId);
}
