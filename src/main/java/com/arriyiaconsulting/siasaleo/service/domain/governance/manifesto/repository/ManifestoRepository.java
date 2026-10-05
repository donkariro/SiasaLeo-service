package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface ManifestoRepository extends BasicRepository<Manifesto, Long> {
    @Insert Manifesto insert(Manifesto entity);
    @Update Manifesto update(Manifesto entity);
    @Query("FROM Manifesto ORDER BY id")
    List<Manifesto> ordered();
}
