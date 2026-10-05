package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface UnitOfMeasureRepository extends BasicRepository<UnitOfMeasure, Long> {
    @Insert UnitOfMeasure insert(UnitOfMeasure entity);
    @Update UnitOfMeasure update(UnitOfMeasure entity);
    @Query("FROM UnitOfMeasure ORDER BY id")
    List<UnitOfMeasure> ordered();
    @Query("FROM UnitOfMeasure WHERE name = :name")
    java.util.Optional<UnitOfMeasure> findByName(@Param("name") String name);
}
