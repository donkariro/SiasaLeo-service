package com.arriyiaconsulting.siasaleo.service.domain.education.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "field_of_study")
public class FieldOfStudy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_name", nullable = false, unique = true, length = 100)
    private String fieldName;

    @Column(length = 255)
    private String description;

    @ManyToOne
    @JoinColumn(name = "parent_field_of_study")
    private FieldOfStudy parent;

    public FieldOfStudy getParent() { return parent; }

    protected FieldOfStudy() {}

    public Long getId() { return id; }
    public String getFieldName() { return fieldName; }
    public String getDescription() { return description; }
}
