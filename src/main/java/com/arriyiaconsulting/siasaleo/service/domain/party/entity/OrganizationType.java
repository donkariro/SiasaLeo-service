package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Business classification of an organization (POLITICAL_PARTY so far); seeded
 * by V18. Distinct from Party's party_type discriminator, which records the
 * structural subtype rather than what the organization does.
 */
@Entity
@Table(name = "organization_type")
public class OrganizationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false, unique = true, length = 100)
    private String typeName;

    @Column(length = 255)
    private String description;

    protected OrganizationType() {
    }

    public Long getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDescription() {
        return description;
    }
}
