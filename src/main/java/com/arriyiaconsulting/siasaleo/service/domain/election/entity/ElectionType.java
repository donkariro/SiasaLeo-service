package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import jakarta.persistence.*;

/** Reference data maintained by database migrations. */
@Entity
@Table(name = "election_type")
public class ElectionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;
    @Column(length = 255)
    private String description;

    protected ElectionType() { }

    public Long getId() { return id; }
    public String getTypeName() { return typeName; }
    public String getDescription() { return description; }
}
