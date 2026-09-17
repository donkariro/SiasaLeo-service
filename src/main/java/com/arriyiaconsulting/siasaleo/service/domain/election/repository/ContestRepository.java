package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Param;
import jakarta.data.page.PageRequest;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRepository extends BasicRepository<Contest, Long> {
    @Query("FROM Contest WHERE electionEventId = :eventId AND (:seatId IS NULL OR seatId = :seatId) ORDER BY id")
    List<Contest> search(@Param("eventId") Long eventId, @Param("seatId") Long seatId, PageRequest pageRequest);

    @Query("FROM Contest WHERE electionEventId = :eventId AND seatId = :seatId")
    Optional<Contest> findByEventAndSeat(@Param("eventId") Long eventId, @Param("seatId") Long seatId);
}
