package com.arriyiaconsulting.siasaleo.service.domain.education.repository;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.FieldOfStudy;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface FieldOfStudyRepository extends BasicRepository<FieldOfStudy, Long> {
    @Query("FROM FieldOfStudy ORDER BY fieldName, id")
    List<FieldOfStudy> findAllOrdered();
}
