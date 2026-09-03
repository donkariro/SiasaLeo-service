package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * An organization's official name over a validity period (V17). uptoDate is
 * null while the name is in force, and organization.official_name points at
 * that row; renaming closes the old row and opens a new one.
 */
@Entity
@Table(name = "organization_name")
public class OrganizationName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    // Null where the register does not say when the name took effect; the
    // parties seeded by V10 take their ORPP registration date.
    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "upto_date")
    private LocalDate uptoDate;

    protected OrganizationName() {
    }

    public OrganizationName(String name, LocalDate fromDate) {
        this.name = name;
        this.fromDate = fromDate;
    }

    /** Closes this name because the organization has taken another one. */
    public void retire(LocalDate uptoDate) {
        this.uptoDate = uptoDate;
    }

    public boolean isCurrent() {
        return uptoDate == null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getUptoDate() {
        return uptoDate;
    }
}
