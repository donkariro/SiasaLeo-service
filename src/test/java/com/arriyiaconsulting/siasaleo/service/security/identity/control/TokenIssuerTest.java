package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.dto.TokenVerification;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for token verification. The signature checks matter most: these
 * cover a straight forgery, a payload edited under a valid signature, a token
 * signed with a different secret, and the alg:none and algorithm-confusion
 * shapes that the fixed-header rule is there to stop.
 */
class TokenIssuerTest {

    private static final String SECRET = "a-test-secret-of-at-least-32-characters";
    private static final String ISSUER = "siasaleo";
    private static final long ACCOUNT_ID = 7L;

    @Test
    void aFreshTokenVerifiesAndCarriesTheAccountAndItsRoles() {
        TokenIssuer issuer = issuerWith(SECRET, 60);
        String token = issuer.issue(account(), Set.of("USER", "ADMINISTRATOR")).token();

        TokenVerification.VerifiedToken verified =
                assertInstanceOf(TokenVerification.Valid.class, issuer.verify(token)).token();

        assertEquals(ACCOUNT_ID, verified.accountId());
        assertEquals("amina@example.com", verified.upn());
        assertEquals(Set.of("USER", "ADMINISTRATOR"), verified.roles());
    }

    @Test
    void anAccountWithNoRolesVerifiesWithNoRoles() {
        TokenIssuer issuer = issuerWith(SECRET, 60);
        String token = issuer.issue(account(), Set.of()).token();

        assertEquals(Set.of(),
                assertInstanceOf(TokenVerification.Valid.class, issuer.verify(token))
                        .token().roles());
    }

    // Expiry is the one refusal a client can act on by logging in again, so it
    // stays distinguishable from every other failure.
    @Test
    void anExpiredTokenIsReportedAsExpiredRatherThanInvalid() {
        TokenIssuer issuer = issuerWith(SECRET, -1);
        String token = issuer.issue(account(), Set.of("USER")).token();

        assertInstanceOf(TokenVerification.Expired.class, issuer.verify(token));
    }

    @Test
    void aPayloadEditedUnderAValidSignatureIsRejected() {
        TokenIssuer issuer = issuerWith(SECRET, 60);
        String[] segments = issuer.issue(account(), Set.of("USER")).token().split("\\.");
        String tampered = base64Url(new String(Base64.getUrlDecoder().decode(segments[1]),
                StandardCharsets.UTF_8).replace("\"groups\":[\"USER\"]",
                "\"groups\":[\"ADMINISTRATOR\"]"));

        TokenVerification result =
                issuer.verify(segments[0] + "." + tampered + "." + segments[2]);

        assertEquals("Signature mismatch",
                assertInstanceOf(TokenVerification.Invalid.class, result).reason());
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        String token = issuerWith("a-different-secret-of-at-least-32-chars", 60)
                .issue(account(), Set.of("ADMINISTRATOR")).token();

        assertInstanceOf(TokenVerification.Invalid.class, issuerWith(SECRET, 60).verify(token));
    }

    // The classic unsigned-token forgery: swap the header for alg:none and
    // drop the signature. Requiring the header verbatim refuses it before any
    // claim is read.
    @Test
    void anAlgNoneTokenIsRejected() {
        TokenIssuer issuer = issuerWith(SECRET, 60);
        String[] segments = issuer.issue(account(), Set.of("USER")).token().split("\\.");
        String unsigned = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}")
                + "." + segments[1] + ".";

        assertEquals("Unexpected JOSE header",
                assertInstanceOf(TokenVerification.Invalid.class, issuer.verify(unsigned))
                        .reason());
    }

    @Test
    void aTokenClaimingAnotherAlgorithmIsRejected() {
        TokenIssuer issuer = issuerWith(SECRET, 60);
        String[] segments = issuer.issue(account(), Set.of("USER")).token().split("\\.");
        String confused = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}")
                + "." + segments[1] + "." + segments[2];

        assertEquals("Unexpected JOSE header",
                assertInstanceOf(TokenVerification.Invalid.class, issuer.verify(confused))
                        .reason());
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() {
        TokenIssuer other = issuerWith(SECRET, 60);
        setField(other, "issuer", "somewhere-else");
        String token = other.issue(account(), Set.of("USER")).token();

        assertEquals("Wrong issuer",
                assertInstanceOf(TokenVerification.Invalid.class,
                        issuerWith(SECRET, 60).verify(token)).reason());
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        TokenIssuer issuer = issuerWith(SECRET, 60);

        assertInstanceOf(TokenVerification.Invalid.class, issuer.verify("not-a-token"));
        assertInstanceOf(TokenVerification.Invalid.class, issuer.verify("a.b.c"));
        assertInstanceOf(TokenVerification.Invalid.class, issuer.verify(""));
    }

    @Test
    void aSecretShorterThanTheHmacBlockIsRefusedAtStartup() {
        TokenIssuer issuer = new TokenIssuer();
        setField(issuer, "configuredSecret", Optional.of("too-short"));

        IllegalStateException thrown =
                assertThrowsIllegalState(issuer);
        assertTrue(thrown.getMessage().contains("at least 32 characters"));
    }

    private static IllegalStateException assertThrowsIllegalState(TokenIssuer issuer) {
        try {
            issuer.initKey();
            throw new AssertionError("Expected an IllegalStateException");
        } catch (IllegalStateException expected) {
            return expected;
        }
    }

    private static UserAccount account() {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getIdentifier()).thenReturn(new Identifier.Email("amina@example.com"));
        return account;
    }

    /**
     * The config values are injected fields, so a unit test sets them directly
     * and runs the @PostConstruct itself.
     */
    private static TokenIssuer issuerWith(String secret, long ttlMinutes) {
        TokenIssuer issuer = new TokenIssuer();
        setField(issuer, "configuredSecret", Optional.of(secret));
        setField(issuer, "ttlMinutes", ttlMinutes);
        setField(issuer, "issuer", ISSUER);
        issuer.initKey();
        return issuer;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = TokenIssuer.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not set " + name, e);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
