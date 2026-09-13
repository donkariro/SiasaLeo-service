package com.arriyiaconsulting.siasaleo.service.security.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * A login identity (V22). Created PENDING_ACTIVATION with exactly one contact
 * point — email or phone, stored normalized — and becomes ACTIVE once the
 * owner proves control of it with a verification code. username and the
 * person link are filled in later profile steps. The login-hardening columns
 * (failed attempts, lockout) are driven by AuthenticationService: repeated
 * password failures set a temporary locked_until window, distinct from the
 * administrative LOCKED status.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain FK value, not a mapped association: keeps security decoupled from
    // the party domain's Person entity.
    @Column(name = "person_id")
    private Long personId;

    private String username;

    @Column(updatable = false)
    private String email;

    @Column(updatable = false)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected UserAccount() {
    }

    public UserAccount(Identifier identifier, String passwordHash) {
        switch (identifier) {
            case Identifier.Email(String value) -> this.email = value;
            case Identifier.Phone(String value) -> this.phone = value;
        }
        this.passwordHash = passwordHash;
        this.status = AccountStatus.PENDING_ACTIVATION;
    }

    @PrePersist
    void onPersist() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void activate() {
        status = AccountStatus.ACTIVE;
    }

    /**
     * Attaches this account to the person it represents — done once, when the
     * owner first declares a role that needs a party-model identity (see
     * PersonProfileService). Re-linking is refused rather than ignored: an
     * account that changed person would silently carry its voter registration,
     * candidacies and claims over to a different human.
     */
    public void linkPerson(Long person) {
        if (personId != null && !personId.equals(person)) {
            throw new IllegalStateException(
                    "Account " + id + " is already linked to person " + personId);
        }
        personId = person;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /** Inside the temporary lockout window set by too many password failures? */
    public boolean isTemporarilyLocked(OffsetDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Counts a wrong password; once maxAttempts is reached the account is
     * locked for lockFor and the counter restarts, so the lockout window
     * expiring grants a fresh set of attempts.
     */
    public void recordFailedLogin(int maxAttempts, Duration lockFor, OffsetDateTime now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockFor);
            failedLoginAttempts = 0;
        }
    }

    public void recordSuccessfulLogin(OffsetDateTime now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    /** The contact point this account registered with, as the domain sum type. */
    public Identifier getIdentifier() {
        return email != null ? new Identifier.Email(email) : new Identifier.Phone(phone);
    }

    public Long getId() {
        return id;
    }

    public Long getPersonId() {
        return personId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
