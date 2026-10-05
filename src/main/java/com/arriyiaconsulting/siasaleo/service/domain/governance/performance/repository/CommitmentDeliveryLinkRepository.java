package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface CommitmentDeliveryLinkRepository extends BasicRepository<CommitmentDeliveryLink, Long> {
    @Insert CommitmentDeliveryLink insert(CommitmentDeliveryLink entity);
    @Update CommitmentDeliveryLink update(CommitmentDeliveryLink entity);
    @Query("FROM CommitmentDeliveryLink ORDER BY id")
    List<CommitmentDeliveryLink> ordered();
    @Query("FROM CommitmentDeliveryLink WHERE commitmentRevisionId = :id ORDER BY id")
    List<CommitmentDeliveryLink> forRevision(@Param("id") Long id);
}
