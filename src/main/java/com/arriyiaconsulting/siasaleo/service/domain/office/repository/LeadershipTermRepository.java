package com.arriyiaconsulting.siasaleo.service.domain.office.repository;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface LeadershipTermRepository extends BasicRepository<LeadershipTerm, Long> {
    @Insert LeadershipTerm insert(LeadershipTerm entity);
    @Update LeadershipTerm update(LeadershipTerm entity);
    @Query("FROM LeadershipTerm ORDER BY id")
    List<LeadershipTerm> ordered();
}
