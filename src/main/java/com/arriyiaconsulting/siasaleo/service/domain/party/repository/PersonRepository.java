package com.arriyiaconsulting.siasaleo.service.domain.party.repository;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface PersonRepository extends BasicRepository<Person, Long> {
}
