package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.dto.TokenVerification;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Issues and verifies the bearer token a successful login returns: a compact
 * JWT signed with HMAC-SHA256. Only this service ever signs or verifies these
 * tokens, so a shared secret suffices — no key-pair management. The secret
 * comes from auth.jwt.secret (min 32 chars); when unset, a random per-startup
 * secret is generated so development works out of the box, at the cost of all
 * tokens dying on restart.
 *
 * Verification requires the header segment to equal the one this service
 * emits, byte for byte. That is stricter than parsing the header and reading
 * its alg, and deliberately so: it rules out alg:none and algorithm-confusion
 * attacks outright rather than by case analysis. The signature is compared in
 * constant time, and only then are the claims read — an unverified payload is
 * attacker-controlled text and is never parsed for meaning.
 */
@ApplicationScoped
public class TokenIssuer {

    private static final Logger LOGGER = Logger.getLogger(TokenIssuer.class.getName());

    // base64url({"alg":"HS256","typ":"JWT"}), the fixed first segment.
    private static final String HEADER = base64Url(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    @Inject
    @ConfigProperty(name = "auth.jwt.secret")
    private Optional<String> configuredSecret;

    @Inject
    @ConfigProperty(name = "auth.jwt.ttl-minutes", defaultValue = "60")
    private long ttlMinutes;

    @Inject
    @ConfigProperty(name = "auth.jwt.issuer", defaultValue = "siasaleo")
    private String issuer;

    private SecretKeySpec key;

    @PostConstruct
    void initKey() {
        byte[] secret;
        if (configuredSecret.isPresent()) {
            if (configuredSecret.get().length() < 32) {
                throw new IllegalStateException(
                        "auth.jwt.secret must be at least 32 characters (256 bits) for HS256");
            }
            secret = configuredSecret.get().getBytes(StandardCharsets.UTF_8);
        } else {
            secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            LOGGER.warning("auth.jwt.secret is not set — using a random per-startup "
                    + "secret; every issued token becomes invalid on restart. "
                    + "Set it for production.");
        }
        key = new SecretKeySpec(secret, "HmacSHA256");
    }

    public record IssuedToken(String token, OffsetDateTime expiresAt) {
    }

    public IssuedToken issue(UserAccount account, Collection<String> roles) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusMinutes(ttlMinutes);
        String payload = Json.createObjectBuilder()
                .add("iss", issuer)
                .add("sub", String.valueOf(account.getId()))
                // upn is the MP-JWT claim for the user principal name; using it
                // now keeps the token forward-compatible with container auth.
                .add("upn", account.getIdentifier().value())
                // groups is the MP-JWT claim @RolesAllowed is checked against.
                .add("groups", groupsOf(roles))
                .add("iat", now.toEpochSecond())
                .add("exp", expiresAt.toEpochSecond())
                .build()
                .toString();
        String signingInput = HEADER + "." + base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return new IssuedToken(signingInput + "." + base64Url(hmac(signingInput)), expiresAt);
    }

    /**
     * Checks a token's signature, issuer and expiry, and returns the claims it
     * carries. Every failure that is not expiry collapses into Invalid: a
     * caller learns only that the token was refused.
     */
    public TokenVerification verify(String token) {
        // Limit -1 so an empty signature still counts as a third segment: an
        // alg:none forgery ends in "." and should be refused on its header,
        // not mislabelled as malformed.
        String[] segments = token.split("\\.", -1);
        if (segments.length != 3) {
            return new TokenVerification.Invalid("Malformed token");
        }
        if (!HEADER.equals(segments[0])) {
            return new TokenVerification.Invalid("Unexpected JOSE header");
        }

        String signingInput = segments[0] + "." + segments[1];
        byte[] presented;
        try {
            presented = Base64.getUrlDecoder().decode(segments[2]);
        } catch (IllegalArgumentException e) {
            return new TokenVerification.Invalid("Signature is not base64url");
        }
        // Constant-time: a length-or-content comparison that short-circuits
        // leaks how much of a forged signature was right.
        if (!MessageDigest.isEqual(hmac(signingInput), presented)) {
            return new TokenVerification.Invalid("Signature mismatch");
        }

        JsonObject claims;
        try {
            claims = Json.createReader(new StringReader(new String(
                    Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8)))
                    .readObject();
        } catch (RuntimeException e) {
            return new TokenVerification.Invalid("Claims are not a JSON object");
        }

        if (!issuer.equals(claims.getString("iss", null))) {
            return new TokenVerification.Invalid("Wrong issuer");
        }
        if (!claims.containsKey("exp") || !claims.containsKey("sub")) {
            return new TokenVerification.Invalid("Missing exp or sub");
        }
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(claims.getJsonNumber("exp").longValue()), ZoneOffset.UTC);
        if (!OffsetDateTime.now().isBefore(expiresAt)) {
            return new TokenVerification.Expired();
        }

        Long accountId;
        try {
            accountId = Long.valueOf(claims.getString("sub"));
        } catch (RuntimeException e) {
            return new TokenVerification.Invalid("sub is not an account id");
        }
        return new TokenVerification.Valid(new TokenVerification.VerifiedToken(
                accountId, claims.getString("upn", null), rolesIn(claims), expiresAt));
    }

    private static JsonArrayBuilder groupsOf(Collection<String> roles) {
        JsonArrayBuilder groups = Json.createArrayBuilder();
        roles.forEach(groups::add);
        return groups;
    }

    private static Set<String> rolesIn(JsonObject claims) {
        if (!claims.containsKey("groups")
                || claims.get("groups").getValueType() != JsonValue.ValueType.ARRAY) {
            return Set.of();
        }
        return claims.getJsonArray("groups").stream()
                .filter(JsonString.class::isInstance)
                .map(value -> ((JsonString) value).getString())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
