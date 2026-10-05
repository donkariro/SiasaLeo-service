package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface WorkEffortRepository extends BasicRepository<WorkEffort, Long> {
    @Insert WorkEffort insert(WorkEffort entity);
    @Update WorkEffort update(WorkEffort entity);
    @Query("FROM WorkEffort ORDER BY id")
    List<WorkEffort> ordered();
}
