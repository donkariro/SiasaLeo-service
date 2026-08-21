package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import java.util.Map;

/**
 * The single place that configures password hashing, so registration and
 * login can never drift apart on work factor. verify() reads its parameters
 * from the stored hash string, so raising these settings keeps old hashes
 * verifiable.
 */
@ApplicationScoped
public class PasswordHasher {

    @Inject
    private Pbkdf2PasswordHash hash;

    private String dummyHash;

    @PostConstruct
    void init() {
        // OWASP-recommended work factor for PBKDF2-HMAC-SHA512.
        hash.initialize(Map.of(
                "Pbkdf2PasswordHash.Algorithm", "PBKDF2WithHmacSHA512",
                "Pbkdf2PasswordHash.Iterations", "210000"));
        dummyHash = hash.generate("timing-equalizer".toCharArray());
    }

    public String generate(String password) {
        return hash.generate(password.toCharArray());
    }

    public boolean verify(String password, String storedHash) {
        return hash.verify(password.toCharArray(), storedHash);
    }

    /**
     * Burns the same time as a real verification. Called when no account
     * matches the identifier, so response timing does not reveal whether an
     * account exists.
     */
    public void verifyAgainstDummy(String password) {
        hash.verify(password.toCharArray(), dummyHash);
    }
}
