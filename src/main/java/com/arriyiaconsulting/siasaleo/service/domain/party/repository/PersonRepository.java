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

    /**
     * Name search for the claim flow, excluding anyone already linked to an
     * account before pagination so pages are never under-filled or empty due
     * to post-filtering.
     */
    @Query("""
           FROM Person p
           WHERE (LOWER(p.firstName) LIKE :pattern OR LOWER(p.lastName) LIKE :pattern)
             AND NOT EXISTS (SELECT 1 FROM UserAccount u WHERE u.personId = p.id)
           ORDER BY p.lastName, p.firstName, p.id
           """)
    List<Person> searchClaimableByName(@Param("pattern") String pattern, PageRequest pageRequest);
}
