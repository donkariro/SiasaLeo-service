package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.dto.LoginRequest;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.LoginResult;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Login rules that only these tests enforce: the enumeration-resistant
 * ordering (dummy hash on unknown accounts, status revealed only after a
 * correct password) and the failed-attempt lockout window.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String EMAIL = "jane@example.com";

    @Mock
    private UserAccountRepository accounts;

    @Mock
    private PasswordHasher passwords;

    @Mock
    private TokenIssuer tokens;

    @InjectMocks
    private AuthenticationService service;

    @Test
    void correctPasswordOnActiveAccountReturnsToken() {
        UserAccount account = activeAccount();
        when(accounts.findByIdentifier(new Identifier.Email(EMAIL)))
                .thenReturn(Optional.of(account));
        when(passwords.verify("s3cret-pw", "stored-hash")).thenReturn(true);
        when(accounts.save(account)).thenReturn(account);
        OffsetDateTime expiry = OffsetDateTime.now().plusHours(1);
        when(tokens.issue(account)).thenReturn(new TokenIssuer.IssuedToken("the-token", expiry));

        LoginResult result = service.login(new LoginRequest(EMAIL, null, "s3cret-pw"));

        LoginResult.Success success = assertInstanceOf(LoginResult.Success.class, result);
        assertEquals("the-token", success.token());
        assertEquals(expiry, success.tokenExpiresAt());
        assertEquals(0, account.getFailedLoginAttempts());
        assertNotNull(account.getLastLoginAt());
    }

    @Test
    void unknownAccountBurnsDummyHashAndSaysInvalidCredentials() {
        when(accounts.findByIdentifier(new Identifier.Email(EMAIL)))
                .thenReturn(Optional.empty());

        LoginResult result = service.login(new LoginRequest(EMAIL, null, "whatever"));

        assertInstanceOf(LoginResult.InvalidCredentials.class, result);
        verify(passwords).verifyAgainstDummy("whatever");
    }

    @Test
    void malformedIdentifierIsIndistinguishableFromWrongPassword() {
        LoginResult result = service.login(new LoginRequest("not-an-email", null, "whatever"));

        assertInstanceOf(LoginResult.InvalidCredentials.class, result);
        verify(passwords).verifyAgainstDummy("whatever");
    }

    @Test
    void repeatedWrongPasswordsLockTheAccount() {
        UserAccount account = activeAccount();
        when(accounts.findByIdentifier(new Identifier.Email(EMAIL)))
                .thenReturn(Optional.of(account));
        when(passwords.verify(anyString(), anyString())).thenReturn(false);
        when(accounts.save(account)).thenReturn(account);

        for (int attempt = 1; attempt <= AuthenticationService.MAX_FAILED_LOGINS; attempt++) {
            assertInstanceOf(LoginResult.InvalidCredentials.class,
                    service.login(new LoginRequest(EMAIL, null, "wrong")));
        }
        assertTrue(account.isTemporarilyLocked(OffsetDateTime.now()));

        LoginResult whileLocked = service.login(new LoginRequest(EMAIL, null, "wrong"));

        LoginResult.TemporarilyLocked locked =
                assertInstanceOf(LoginResult.TemporarilyLocked.class, whileLocked);
        assertNotNull(locked.until());
    }

    @Test
    void correctPasswordOnPendingAccountSaysNotVerifiedWithoutToken() {
        UserAccount account = pendingAccount();
        when(accounts.findByIdentifier(new Identifier.Email(EMAIL)))
                .thenReturn(Optional.of(account));
        when(passwords.verify("s3cret-pw", "stored-hash")).thenReturn(true);

        LoginResult result = service.login(new LoginRequest(EMAIL, null, "s3cret-pw"));

        assertInstanceOf(LoginResult.NotVerified.class, result);
        verify(tokens, never()).issue(any());
    }

    @Test
    void wrongPasswordOnPendingAccountDoesNotRevealItsStatus() {
        UserAccount account = pendingAccount();
        when(accounts.findByIdentifier(new Identifier.Email(EMAIL)))
                .thenReturn(Optional.of(account));
        when(passwords.verify("wrong", "stored-hash")).thenReturn(false);
        when(accounts.save(account)).thenReturn(account);

        LoginResult result = service.login(new LoginRequest(EMAIL, null, "wrong"));

        assertInstanceOf(LoginResult.InvalidCredentials.class, result);
        assertEquals(1, account.getFailedLoginAttempts());
    }

    private static UserAccount pendingAccount() {
        return new UserAccount(new Identifier.Email(EMAIL), "stored-hash");
    }

    private static UserAccount activeAccount() {
        UserAccount account = pendingAccount();
        account.activate();
        return account;
    }
}
