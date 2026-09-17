package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import jakarta.persistence.*;

/** Reference data maintained by database migrations. */
@Entity
@Table(name = "election_status")
public class ElectionStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "status_name", nullable = false, unique = true, length = 50)
    private String statusName;
    @Column(length = 255)
    private String description;

    protected ElectionStatus() { }

    public Long getId() { return id; }
    public String getStatusName() { return statusName; }
    public String getDescription() { return description; }
}
