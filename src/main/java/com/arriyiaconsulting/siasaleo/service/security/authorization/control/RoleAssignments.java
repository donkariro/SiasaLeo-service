package com.arriyiaconsulting.siasaleo.service.security.authorization.control;

import com.arriyiaconsulting.siasaleo.service.security.authorization.entity.SecurityRole;
import com.arriyiaconsulting.siasaleo.service.security.authorization.entity.UserSecurityRole;
import com.arriyiaconsulting.siasaleo.service.security.authorization.repository.SecurityRoleRepository;
import com.arriyiaconsulting.siasaleo.service.security.authorization.repository.UserSecurityRoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads and grants the roles behind @RolesAllowed (V23).
 *
 * Roles are resolved once, at login, and travel in the token's groups claim;
 * requests afterwards are authorized from the token alone, so a grant or
 * revocation takes effect on the holder's next login rather than immediately.
 * That is the ordinary trade of stateless tokens, and the reason token TTL is
 * kept short (auth.jwt.ttl-minutes). Anything that must revoke at once — a
 * disabled account, say — needs a check against user_account on each request,
 * not a role.
 */
@ApplicationScoped
public class RoleAssignments {

    @Inject
    private UserSecurityRoleRepository grants;

    @Inject
    private SecurityRoleRepository roles;

    /** Role names held by an account, for the token's groups claim. */
    public Set<String> rolesOf(Long accountId) {
        return grants.findByAccount(accountId).stream()
                .map(grant -> grant.getSecurityRole().getRoleName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Grants USER to a newly verified account. Without it a verified account
     * holds no role and every @RolesAllowed endpoint refuses it, so this runs
     * as part of verification rather than as a separate administrative step.
     * Idempotent: re-verifying does not duplicate the grant, which the UNIQUE
     * on (user_account_id, security_role_id) would reject anyway.
     */
    @Transactional
    public void grantDefaultRole(Long accountId) {
        grant(accountId, SecurityRole.USER);
    }

    @Transactional
    public void grant(Long accountId, String roleName) {
        if (rolesOf(accountId).contains(roleName)) {
            return;
        }
        SecurityRole role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + roleName + " is not defined; V23 seeds it"));
        grants.save(new UserSecurityRole(accountId, role));
    }
}
