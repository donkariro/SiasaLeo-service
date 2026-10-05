package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface GoalRepository extends BasicRepository<Goal, Long> {
    @Insert Goal insert(Goal entity);
    @Update Goal update(Goal entity);
    @Query("FROM Goal ORDER BY id")
    List<Goal> ordered();
}
