package com.arriyiaconsulting.siasaleo.service.electoralgeography.repository;

import com.arriyiaconsulting.siasaleo.service.electoralgeography.entity.AreaType;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AreaTypeRepository extends BasicRepository<AreaType, Long> {

    @Find
    @OrderBy("id")
    List<AreaType> findAllOrdered();

    @Find
    Optional<AreaType> findByName(@By("name") String name);
}
