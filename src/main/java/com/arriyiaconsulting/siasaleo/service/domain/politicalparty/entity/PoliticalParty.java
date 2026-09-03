package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Organization;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationName;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Political party (V9), seeded from the ORPP register (V10): the third level
 * of the joined hierarchy party -> organization -> political_party, so the id
 * is inherited from Party and the name comes from Organization.
 * <p>
 * Symbol, colours and slogan are not columns here — V20 normalized them into
 * politicalparty_symbol / _color / _slogan, each with its own validity period.
 */
@Entity
@Table(name = "political_party")
@DiscriminatorValue("POLITICAL_PARTY")
public class PoliticalParty extends Organization {

    @Column(length = 50)
    private String abbreviation;

    @Column(name = "registered_on")
    private LocalDate registeredOn;

    @Column(name = "postal_address", length = 255)
    private String postalAddress;

    @Column(name = "head_office_location", length = 255)
    private String headOfficeLocation;

    // Free text from the ORPP register recording earlier names and mergers.
    @Column(length = 255)
    private String changes;

    protected PoliticalParty() {
    }

    public PoliticalParty(OrganizationName officialName, OrganizationType orgType,
                          String registrationNumber, String abbreviation,
                          LocalDate registeredOn, String postalAddress,
                          String headOfficeLocation, String changes) {
        super(officialName, orgType, registrationNumber);
        this.abbreviation = abbreviation;
        this.registeredOn = registeredOn;
        this.postalAddress = postalAddress;
        this.headOfficeLocation = headOfficeLocation;
        this.changes = changes;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public LocalDate getRegisteredOn() {
        return registeredOn;
    }

    public String getPostalAddress() {
        return postalAddress;
    }

    public String getHeadOfficeLocation() {
        return headOfficeLocation;
    }

    public String getChanges() {
        return changes;
    }
}
