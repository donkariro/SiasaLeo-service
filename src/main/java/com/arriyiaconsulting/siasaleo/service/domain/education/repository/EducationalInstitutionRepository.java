package com.arriyiaconsulting.siasaleo.service.domain.education.repository;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.EducationalInstitution;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface EducationalInstitutionRepository extends BasicRepository<EducationalInstitution, Long> {
    @Query("FROM EducationalInstitution WHERE (:typeId IS NULL OR institutionType.id = :typeId) AND (:search IS NULL OR LOWER(officialName.name) LIKE :search) ORDER BY officialName.name, id")
    List<EducationalInstitution> search(@Param("typeId") Long typeId, @Param("search") String search,
                                       PageRequest pageRequest);
}
