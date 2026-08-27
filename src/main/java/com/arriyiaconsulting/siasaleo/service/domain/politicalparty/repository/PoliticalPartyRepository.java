package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface PoliticalPartyRepository extends BasicRepository<PoliticalParty, Long> {
}
