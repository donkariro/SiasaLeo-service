package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionStatus;
import jakarta.data.repository.*;
import java.util.List;
import java.util.Optional;

@Repository
public interface ElectionStatusRepository extends BasicRepository<ElectionStatus, Long> {
    @Query("FROM ElectionStatus ORDER BY id")
    List<ElectionStatus> findAllOrdered();

    @Query("FROM ElectionStatus WHERE statusName = :name")
    Optional<ElectionStatus> findByStatusName(@Param("name") String name);
}
