package com.arriyiaconsulting.siasaleo.service.security.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * One-time code sent to an account's email/phone during registration. Only
 * the SHA-256 of the code is stored; a row is spent either by being consumed
 * (successful verification), expiring, or running out of attempts.
 */
@Entity
@Table(name = "verification_code")
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id")
    private UserAccount userAccount;

    @Column(name = "code_hash", nullable = false, updatable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected VerificationCode() {
    }

    public VerificationCode(UserAccount userAccount, String codeHash, OffsetDateTime expiresAt) {
        this.userAccount = userAccount;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onPersist() {
        createdAt = OffsetDateTime.now();
    }

    public boolean isExpired(OffsetDateTime now) {
        return now.isAfter(expiresAt);
    }

    public void recordFailedAttempt() {
        attempts++;
    }

    public void consume() {
        consumedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getAttempts() {
        return attempts;
    }
}
