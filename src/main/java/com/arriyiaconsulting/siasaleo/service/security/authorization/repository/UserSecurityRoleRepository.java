package com.arriyiaconsulting.siasaleo.service.security.authorization.repository;

import com.arriyiaconsulting.siasaleo.service.security.authorization.entity.UserSecurityRole;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface UserSecurityRoleRepository extends BasicRepository<UserSecurityRole, Long> {

    // Read once per login, to stamp the token's groups claim; requests
    // afterwards read the roles from the token rather than from here.
    @Query("FROM UserSecurityRole WHERE userAccountId = :accountId")
    List<UserSecurityRole> findByAccount(@Param("accountId") Long accountId);
}
