package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface PerformanceTargetRepository extends BasicRepository<PerformanceTarget, Long> {
    @Insert PerformanceTarget insert(PerformanceTarget entity);
    @Update PerformanceTarget update(PerformanceTarget entity);
    @Query("FROM PerformanceTarget ORDER BY id")
    List<PerformanceTarget> ordered();
}
