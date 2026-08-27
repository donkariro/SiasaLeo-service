package com.arriyiaconsulting.siasaleo.service.domain.candidate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stage of the candidacy lifecycle (EXPRESSED_INTEREST .. NOT_ELECTED);
 * seeded in lifecycle order by V21.
 */
@Entity
@Table(name = "candidacy_status")
public class CandidacyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_name", nullable = false, unique = true, length = 50)
    private String statusName;

    @Column(length = 255)
    private String description;

    protected CandidacyStatus() {
    }

    public Long getId() {
        return id;
    }

    public String getStatusName() {
        return statusName;
    }

    public String getDescription() {
        return description;
    }
}
