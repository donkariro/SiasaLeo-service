package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface MeasurementScopeRepository extends BasicRepository<MeasurementScope, Long> {
    @Insert MeasurementScope insert(MeasurementScope entity);
    @Update MeasurementScope update(MeasurementScope entity);
    @Query("FROM MeasurementScope ORDER BY id")
    List<MeasurementScope> ordered();
}
