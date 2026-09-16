package com.arriyiaconsulting.siasaleo.service.domain.education.repository;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.EducationalInstitutionType;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface EducationalInstitutionTypeRepository extends BasicRepository<EducationalInstitutionType, Long> {
    @Query("FROM EducationalInstitutionType ORDER BY typeName, id")
    List<EducationalInstitutionType> findAllOrdered();
}
