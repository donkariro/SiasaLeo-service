package com.arriyiaconsulting.siasaleo.service.domain.candidate.repository;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.Candidacy;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidacyRepository extends BasicRepository<Candidacy, Long> {

    @Query("FROM Candidacy WHERE contest.id = :contestId AND sourceReference = :source AND sourceRecordReference = :record")
    Optional<Candidacy> findBySource(@Param("contestId") Long contestId, @Param("source") String source, @Param("record") String record);

    @Query("FROM Candidacy WHERE person.id = :personId AND contest.id = :contestId")
    Optional<Candidacy> findByPersonAndContest(@Param("personId") Long personId,
                                               @Param("contestId") Long contestId);

    @Query("FROM Candidacy WHERE contest.id = :contestId ORDER BY id")
    List<Candidacy> findByContest(@Param("contestId") Long contestId, PageRequest pageRequest);
}
