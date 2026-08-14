package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface ElectoralAreaRepository extends BasicRepository<ElectoralArea, Long> {

    @Query("FROM ElectoralArea WHERE areaType.name = :type ORDER BY id")
    List<ElectoralArea> findByTypeName(@Param("type") String type, PageRequest pageRequest);

    @Find
    @OrderBy("id")
    List<ElectoralArea> findChildren(@By("parentId") Long parentId);

    // prefix must end with '/%' so idx_electoral_area_ancestor_path
    // (varchar_pattern_ops) can serve the prefix match.
    @Query("FROM ElectoralArea WHERE ancestorPath LIKE :prefix ORDER BY id")
    List<ElectoralArea> findDescendants(@Param("prefix") String prefix, PageRequest pageRequest);
}
