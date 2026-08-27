package com.arriyiaconsulting.siasaleo.service.domain.voter.repository;

import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistration;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistrationStatus;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoterRegistrationRepository extends BasicRepository<VoterRegistration, Long> {

    // Only ever called with ACTIVE, where idx_voter_registration_active caps
    // the result at one row; the retired statuses can repeat per person and
    // belong in findByPerson instead.
    @Query("FROM VoterRegistration WHERE person.id = :personId AND status = :status")
    Optional<VoterRegistration> findByPersonAndStatus(@Param("personId") Long personId,
                                                      @Param("status") VoterRegistrationStatus status);

    // Newest first: the current registration leads, retired ones trail it.
    @Query("FROM VoterRegistration WHERE person.id = :personId ORDER BY id DESC")
    List<VoterRegistration> findByPerson(@Param("personId") Long personId);

    @Query("FROM VoterRegistration WHERE registrationCenter.id = :centerId AND status = :status ORDER BY id")
    List<VoterRegistration> findByCenterAndStatus(@Param("centerId") Long centerId,
                                                  @Param("status") VoterRegistrationStatus status,
                                                  PageRequest pageRequest);
}
