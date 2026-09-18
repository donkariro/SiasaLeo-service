package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.mapping.IdentityMapper;
import com.arriyiaconsulting.siasaleo.service.security.authorization.control.RoleAssignments;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.LoginRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.LoginResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Login flow. Checks run strictly in this order: lockout window first (so a
 * locked account leaks nothing about the password), then the password, then
 * account status — an unverified or disabled account reveals its state only
 * to someone who already knows the password. Every outcome is a LoginResult
 * variant; this service never throws for business rules.
 */
@ApplicationScoped
public class AuthenticationService {

    @Inject
    private IdentityMapper identityMapper;

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Inject
    private UserAccountRepository accounts;

    @Inject
    private PasswordHasher passwords;

    @Inject
    private TokenIssuer tokens;

    @Inject
    private RoleAssignments roleAssignments;

    @Transactional
    public LoginResult login(LoginRequest request) {
        return switch (Identifier.parseOneOf(request.email(), request.phone())) {
            case Identifier.Invalid ignored -> {
                // Same cost as a real check, same answer as a wrong password.
                passwords.verifyAgainstDummy(request.password());
                yield new LoginResult.InvalidCredentials();
            }
            case Identifier.Parsed(Identifier identifier) ->
                    loginWith(identifier, request.password());
        };
    }

    private LoginResult loginWith(Identifier identifier, String password) {
        Optional<UserAccount> found = accounts.findByIdentifier(identifier);
        if (found.isEmpty()) {
            passwords.verifyAgainstDummy(password);
            return new LoginResult.InvalidCredentials();
        }
        UserAccount account = found.get();
        OffsetDateTime now = OffsetDateTime.now();
        if (account.isTemporarilyLocked(now)) {
            return new LoginResult.TemporarilyLocked(account.getLockedUntil());
        }
        if (!passwords.verify(password, account.getPasswordHash())) {
            account.recordFailedLogin(MAX_FAILED_LOGINS, LOCK_DURATION, now);
            accounts.save(account);
            return new LoginResult.InvalidCredentials();
        }
        return switch (account.getStatus()) {
            case PENDING_ACTIVATION -> new LoginResult.NotVerified();
            case LOCKED, DISABLED -> new LoginResult.AccountUnavailable();
            case ACTIVE -> {
                account.recordSuccessfulLogin(now);
                UserAccount saved = accounts.save(account);
                // Roles are read once, here, and ride in the token's groups
                // claim; see RoleAssignments for what that costs.
                TokenIssuer.IssuedToken issued =
                        tokens.issue(saved, roleAssignments.rolesOf(saved.getId()));
                yield new LoginResult.Success(
                        identityMapper.toUserAccountDto(saved), issued.token(), issued.expiresAt());
            }
        };
    }
}
