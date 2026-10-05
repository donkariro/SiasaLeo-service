package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface WorkEffortTypeRepository extends BasicRepository<WorkEffortType, String> {
    @Insert WorkEffortType insert(WorkEffortType entity);
    @Update WorkEffortType update(WorkEffortType entity);
    @Query("FROM WorkEffortType ORDER BY code")
    List<WorkEffortType> ordered();
}
