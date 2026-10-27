package com.arriyiaconsulting.siasaleo.service.domain.nomination.repository;

import com.arriyiaconsulting.siasaleo.service.domain.nomination.entity.NominationExercise;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface NominationExerciseRepository extends BasicRepository<NominationExercise, Long> {
    @Insert NominationExercise insert(NominationExercise entity);
    @Update NominationExercise update(NominationExercise entity);
    @Query("FROM NominationExercise WHERE politicalPartyId = :party ORDER BY opensAt DESC, id DESC")
    List<NominationExercise> forParty(@Param("party") Long party);
}
