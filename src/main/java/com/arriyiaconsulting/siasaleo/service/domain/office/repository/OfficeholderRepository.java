package com.arriyiaconsulting.siasaleo.service.domain.office.repository;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface OfficeholderRepository extends BasicRepository<Officeholder, Long> {
    @Insert Officeholder insert(Officeholder entity);
    @Update Officeholder update(Officeholder entity);
    @Query("FROM Officeholder ORDER BY id")
    List<Officeholder> ordered();
}
