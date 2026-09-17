package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionCycle;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface ElectionCycleRepository extends BasicRepository<ElectionCycle, Long> {
    @Query("FROM ElectionCycle ORDER BY fromYear, id")
    List<ElectionCycle> findAllOrdered();
}
