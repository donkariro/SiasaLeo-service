package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySymbol;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoliticalPartySymbolRepository
        extends BasicRepository<PoliticalPartySymbol, Long> {

    // idx_politicalparty_symbol_current caps this at one row per party.
    @Query("FROM PoliticalPartySymbol WHERE politicalParty.id = :partyId AND uptoDate IS NULL")
    Optional<PoliticalPartySymbol> findCurrentByParty(@Param("partyId") Long partyId);

    // The succession, most recent first. NULLS LAST keeps symbols whose start
    // the register never recorded from leading the list. A party has had only
    // a handful of symbols, so this is not paged.
    @Query("FROM PoliticalPartySymbol WHERE politicalParty.id = :partyId "
            + "ORDER BY fromDate DESC NULLS LAST, id DESC")
    List<PoliticalPartySymbol> findByParty(@Param("partyId") Long partyId);
}
