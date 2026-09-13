package com.arriyiaconsulting.siasaleo.service.security.authorization.repository;

import com.arriyiaconsulting.siasaleo.service.security.authorization.entity.SecurityRole;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import java.util.Optional;

@Repository
public interface SecurityRoleRepository extends BasicRepository<SecurityRole, Long> {

    @Find
    Optional<SecurityRole> findByRoleName(@By("roleName") String roleName);
}
