package com.arriyiaconsulting.siasaleo.service.security.authorization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * One role held by one account (V23). userAccountId is a plain FK value rather
 * than a mapped association, matching how UserAccount holds person_id: the
 * grant names the account without either entity owning the other.
 */
@Entity
@Table(name = "user_security_role")
public class UserSecurityRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id", nullable = false, updatable = false)
    private Long userAccountId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "security_role_id", updatable = false)
    private SecurityRole securityRole;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private OffsetDateTime grantedAt;

    protected UserSecurityRole() {
    }

    public UserSecurityRole(Long userAccountId, SecurityRole securityRole) {
        this.userAccountId = userAccountId;
        this.securityRole = securityRole;
    }

    @PrePersist
    void onPersist() {
        grantedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserAccountId() {
        return userAccountId;
    }

    public SecurityRole getSecurityRole() {
        return securityRole;
    }

    public OffsetDateTime getGrantedAt() {
        return grantedAt;
    }
}
