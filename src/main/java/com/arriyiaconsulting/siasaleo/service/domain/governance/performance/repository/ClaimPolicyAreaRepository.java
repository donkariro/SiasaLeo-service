package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ClaimPolicyAreaRepository extends BasicRepository<ClaimPolicyArea, ClaimPolicyAreaId> {
    @Insert ClaimPolicyArea insert(ClaimPolicyArea entity);
    @Update ClaimPolicyArea update(ClaimPolicyArea entity);
    @Query("FROM ClaimPolicyArea ORDER BY recordId, policyAreaId")
    List<ClaimPolicyArea> ordered();
}
