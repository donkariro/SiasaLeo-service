package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.ElectionEvent;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface ElectionEventRepository extends BasicRepository<ElectionEvent, Long> {
    @Query("FROM ElectionEvent WHERE (:cycleId IS NULL OR electionCycle.id = :cycleId) "
            + "AND (:typeId IS NULL OR type.id = :typeId) "
            + "AND (:statusId IS NULL OR status.id = :statusId) ORDER BY electionDate DESC, id DESC")
    List<ElectionEvent> search(@Param("cycleId") Long cycleId, @Param("typeId") Long typeId,
                              @Param("statusId") Long statusId, PageRequest pageRequest);
}
