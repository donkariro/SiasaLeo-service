package com.arriyiaconsulting.siasaleo.service.domain.nomination.repository;

import com.arriyiaconsulting.siasaleo.service.domain.nomination.entity.NominationApplication;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface NominationApplicationRepository extends BasicRepository<NominationApplication, Long> {
    @Insert NominationApplication insert(NominationApplication entity);
    @Update NominationApplication update(NominationApplication entity);
    @Query("FROM NominationApplication WHERE exerciseId = :exercise ORDER BY id")
    List<NominationApplication> forExercise(@Param("exercise") Long exercise);
    @Query("FROM NominationApplication WHERE personId = :person ORDER BY id DESC")
    List<NominationApplication> forPerson(@Param("person") Long person);
}
