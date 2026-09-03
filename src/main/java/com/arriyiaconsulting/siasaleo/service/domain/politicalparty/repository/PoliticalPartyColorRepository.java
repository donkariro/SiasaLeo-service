package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartyColor;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface PoliticalPartyColorRepository
        extends BasicRepository<PoliticalPartyColor, Long> {

    // In register order: display_order is 1-based and carries the meaning, so
    // it is the only sensible ordering.
    @Query("FROM PoliticalPartyColor WHERE politicalParty.id = :partyId ORDER BY displayOrder")
    List<PoliticalPartyColor> findByParty(@Param("partyId") Long partyId);
}
