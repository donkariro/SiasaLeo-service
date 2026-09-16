package com.arriyiaconsulting.siasaleo.service.domain.education.repository;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.EducationLevel;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface EducationLevelRepository extends BasicRepository<EducationLevel, Long> {
    @Query("FROM EducationLevel ORDER BY levelOrder ASC NULLS LAST, id")
    List<EducationLevel> findAllOrdered();
}
