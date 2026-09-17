package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionType;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface ElectionTypeRepository extends BasicRepository<ElectionType, Long> {
    @Query("FROM ElectionType ORDER BY id")
    List<ElectionType> findAllOrdered();
}
