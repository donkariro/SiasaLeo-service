package com.arriyiaconsulting.siasaleo.service.domain.candidate.repository;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.CandidacyStatus;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import java.util.Optional;

@Repository
public interface CandidacyStatusRepository extends BasicRepository<CandidacyStatus, Long> {

    @Find
    Optional<CandidacyStatus> findByStatusName(@By("statusName") String statusName);
}
