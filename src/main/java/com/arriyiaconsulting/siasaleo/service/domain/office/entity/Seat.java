package com.arriyiaconsulting.siasaleo.service.domain.office.entity;

import jakarta.persistence.*;

/** Reference data maintained by database migrations. */
@Entity
@Table(name = "seat")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "electoral_area_id", nullable = false)
    private Long electoralAreaId;
    @Column(name = "office_id", nullable = false)
    private Long officeId;
    @Column(length = 255)
    private String description;

    protected Seat() { }

    public Long getId() { return id; }
    public Long getElectoralAreaId() { return electoralAreaId; }
    public Long getOfficeId() { return officeId; }
    public String getDescription() { return description; }
}
