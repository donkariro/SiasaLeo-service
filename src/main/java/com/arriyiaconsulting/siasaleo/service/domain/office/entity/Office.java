package com.arriyiaconsulting.siasaleo.service.domain.office.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An elective office from the register seeded by V6. */
@Entity
@Table(name = "office")
public class Office {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String abbreviation;

    @Column(length = 255)
    private String description;

    // Matches the database values "National" and "County".
    @Column(nullable = false, length = 10)
    private String level;

    protected Office() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public String getDescription() { return description; }
    public String getLevel() { return level; }
}
