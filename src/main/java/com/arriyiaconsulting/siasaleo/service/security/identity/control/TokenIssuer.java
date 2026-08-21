package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Issues the bearer token a successful login returns: a compact JWT signed
 * with HMAC-SHA256. Only this service ever signs or (later) verifies these
 * tokens, so a shared secret suffices — no key-pair management. The secret
 * comes from auth.jwt.secret (min 32 chars); when unset, a random per-startup
 * secret is generated so development works out of the box, at the cost of all
 * tokens dying on restart.
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

    public IssuedToken issue(UserAccount account) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusMinutes(ttlMinutes);
        String payload = Json.createObjectBuilder()
                .add("iss", issuer)
                .add("sub", String.valueOf(account.getId()))
                // upn is the MP-JWT claim for the user principal name; using it
                // now keeps the token forward-compatible with container auth.
                .add("upn", account.getIdentifier().value())
                .add("iat", now.toEpochSecond())
                .add("exp", expiresAt.toEpochSecond())
                .build()
                .toString();
        String signingInput = HEADER + "." + base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return new IssuedToken(signingInput + "." + base64Url(hmac(signingInput)), expiresAt);
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
