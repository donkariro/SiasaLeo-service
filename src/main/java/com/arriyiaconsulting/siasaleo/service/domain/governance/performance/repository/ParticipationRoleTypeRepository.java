package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ParticipationRoleTypeRepository extends BasicRepository<ParticipationRoleType, String> {
    @Insert ParticipationRoleType insert(ParticipationRoleType entity);
    @Update ParticipationRoleType update(ParticipationRoleType entity);
    @Query("FROM ParticipationRoleType ORDER BY code")
    List<ParticipationRoleType> ordered();
}
