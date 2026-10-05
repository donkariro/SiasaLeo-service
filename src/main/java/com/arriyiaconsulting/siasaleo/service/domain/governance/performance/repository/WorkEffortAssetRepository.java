package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface WorkEffortAssetRepository extends BasicRepository<WorkEffortAsset, WorkEffortAssetId> {
    @Insert WorkEffortAsset insert(WorkEffortAsset entity);
    @Update WorkEffortAsset update(WorkEffortAsset entity);
    @Query("FROM WorkEffortAsset ORDER BY workEffortId, assetVersionId")
    List<WorkEffortAsset> ordered();
}
