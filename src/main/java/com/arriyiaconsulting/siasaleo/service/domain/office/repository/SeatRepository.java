package com.arriyiaconsulting.siasaleo.service.domain.office.repository;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Seat;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface SeatRepository extends BasicRepository<Seat, Long> {
    @Query("FROM Seat WHERE (:officeId IS NULL OR officeId = :officeId) AND (:areaId IS NULL OR electoralAreaId = :areaId) ORDER BY id")
    List<Seat> search(@Param("officeId") Long officeId, @Param("areaId") Long areaId, PageRequest pageRequest);
}
