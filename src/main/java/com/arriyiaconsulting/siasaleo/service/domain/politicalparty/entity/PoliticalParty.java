package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Political party (V9), seeded from the ORPP register (V10). Minimal
 * read-only mapping for now: parties are referenced by candidacies but not
 * yet managed through the API. The party's name lives in organization /
 * organization_name (V17) and is not mapped here.
 */
@Entity
@Table(name = "political_party")
public class PoliticalParty {

    @Id
    private Long id;

    @Column(length = 50)
    private String abbreviation;

    protected PoliticalParty() {
    }

    public Long getId() {
        return id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
