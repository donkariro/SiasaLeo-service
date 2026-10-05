package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentRepository extends BasicRepository<Commitment, Long> {
    @Insert Commitment insert(Commitment entity);
    @Update Commitment update(Commitment entity);
    @Query("FROM Commitment ORDER BY id")
    List<Commitment> ordered();
}
