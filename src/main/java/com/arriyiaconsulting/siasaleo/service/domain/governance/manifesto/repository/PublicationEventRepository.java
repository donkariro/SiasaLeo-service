package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface PublicationEventRepository extends BasicRepository<PublicationEvent, Long> {
    @Insert PublicationEvent insert(PublicationEvent entity);
    @Update PublicationEvent update(PublicationEvent entity);
    @Query("FROM PublicationEvent ORDER BY id")
    List<PublicationEvent> ordered();
}
