package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface PolicyAreaRepository extends BasicRepository<PolicyArea, Long> {
    @Insert PolicyArea insert(PolicyArea entity);
    @Update PolicyArea update(PolicyArea entity);
    @Query("FROM PolicyArea ORDER BY id")
    List<PolicyArea> ordered();
}
