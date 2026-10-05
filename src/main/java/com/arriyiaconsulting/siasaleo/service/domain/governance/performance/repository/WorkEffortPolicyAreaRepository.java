package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface WorkEffortPolicyAreaRepository extends BasicRepository<WorkEffortPolicyArea, WorkEffortPolicyAreaId> {
    @Insert WorkEffortPolicyArea insert(WorkEffortPolicyArea entity);
    @Update WorkEffortPolicyArea update(WorkEffortPolicyArea entity);
    @Query("FROM WorkEffortPolicyArea ORDER BY workEffortId, policyAreaId")
    List<WorkEffortPolicyArea> ordered();
}
