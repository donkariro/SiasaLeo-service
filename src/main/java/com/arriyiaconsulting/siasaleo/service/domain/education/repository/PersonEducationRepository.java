package com.arriyiaconsulting.siasaleo.service.domain.education.repository;

import com.arriyiaconsulting.siasaleo.service.domain.education.entity.PersonEducation;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface PersonEducationRepository extends BasicRepository<PersonEducation, Long> {
    @Query("FROM PersonEducation WHERE person.id = :personId ORDER BY fromDate DESC NULLS LAST, id DESC")
    List<PersonEducation> findByPerson(@Param("personId") Long personId);
}
