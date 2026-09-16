package com.arriyiaconsulting.siasaleo.service.domain.office.repository;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Office;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface OfficeRepository extends BasicRepository<Office, Long> {

    @Query("FROM Office ORDER BY name, id")
    List<Office> findAllOrderedByName();
}
