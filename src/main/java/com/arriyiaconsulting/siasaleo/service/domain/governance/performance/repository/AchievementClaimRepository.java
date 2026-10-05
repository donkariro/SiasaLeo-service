package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface AchievementClaimRepository extends BasicRepository<AchievementClaim, Long> {
    @Insert AchievementClaim insert(AchievementClaim entity);
    @Update AchievementClaim update(AchievementClaim entity);
    @Query("FROM AchievementClaim ORDER BY id")
    List<AchievementClaim> ordered();
}
