package com.arriyiaconsulting.siasaleo.service.domain.party.repository;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface PersonRepository extends BasicRepository<Person, Long> {

    /**
     * Name search behind the claim flow: an aspirant finds the person row the
     * system already holds for them instead of creating a second one. The
     * caller passes an already-lower-cased pattern.
     */
    @Query("""
           FROM Person
           WHERE LOWER(firstName) LIKE :pattern OR LOWER(lastName) LIKE :pattern
           ORDER BY lastName, firstName, id
           """)
    List<Person> searchByName(@Param("pattern") String pattern, PageRequest pageRequest);
}
