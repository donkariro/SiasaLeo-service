package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface PoliticalPartyRepository extends BasicRepository<PoliticalParty, Long> {

    // Ordered by the name in force rather than by id so the register reads
    // alphabetically; officialName is an implicit join onto organization_name
    // (V17). Named apart from BasicRepository.findAll() to avoid overloading it.
    @Query("FROM PoliticalParty ORDER BY officialName.name")
    List<PoliticalParty> findAllOrderedByName(PageRequest pageRequest);
}
