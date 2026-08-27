package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface ContestRepository extends BasicRepository<Contest, Long> {
}
