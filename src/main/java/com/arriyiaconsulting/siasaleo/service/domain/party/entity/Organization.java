package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Organization subtype of the party model (V1): a body rather than a natural
 * person. The id is inherited from Party; this table reuses it as PK + FK.
 * The name is not a column here — it lives in organization_name (V17) with a
 * validity period, and officialName references the row currently in force.
 * PoliticalParty extends this with the attributes of the ORPP register.
 */
@Entity
@Table(name = "organization")
@DiscriminatorValue("ORGANIZATION")
public class Organization extends Party {

    // The ORPP certificate serial number for a political party (V9).
    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    // organization_name has no back-reference, so a name row is only ever
    // reachable through the organization that owns it; cascading the insert
    // keeps callers from having to save the two in the right order.
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "official_name")
    private OrganizationName officialName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_type")
    private OrganizationType orgType;

    protected Organization() {
    }

    public Organization(OrganizationName officialName, OrganizationType orgType,
                        String registrationNumber) {
        this.officialName = officialName;
        this.orgType = orgType;
        this.registrationNumber = registrationNumber;
    }

    /** The name currently in force. */
    public String getName() {
        return officialName.getName();
    }

    public OrganizationName getOfficialName() {
        return officialName;
    }

    public OrganizationType getOrgType() {
        return orgType;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }
}
