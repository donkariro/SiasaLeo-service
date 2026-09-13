package com.arriyiaconsulting.siasaleo.service.security.authorization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A role an account can hold (V23), seeded with ADMINISTRATOR and USER. The
 * role_name is what reaches @RolesAllowed, by way of the token's groups claim.
 */
@Entity
@Table(name = "security_role")
public class SecurityRole {

    /** Granted to every account when it is verified. */
    public static final String USER = "USER";

    public static final String ADMINISTRATOR = "ADMINISTRATOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(length = 255)
    private String description;

    protected SecurityRole() {
    }

    public Long getId() {
        return id;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }
}
