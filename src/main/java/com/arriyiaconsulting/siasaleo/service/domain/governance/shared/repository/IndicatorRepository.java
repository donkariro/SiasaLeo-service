package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface IndicatorRepository extends BasicRepository<Indicator, Long> {
    @Insert Indicator insert(Indicator entity);
    @Update Indicator update(Indicator entity);
    @Query("FROM Indicator ORDER BY id")
    List<Indicator> ordered();
}
