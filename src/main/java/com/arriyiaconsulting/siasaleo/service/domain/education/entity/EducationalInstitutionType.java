package com.arriyiaconsulting.siasaleo.service.domain.education.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "educational_institution_type")
public class EducationalInstitutionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;

    @Column(length = 255)
    private String description;


    protected EducationalInstitutionType() {}

    public Long getId() { return id; }
    public String getTypeName() { return typeName; }
    public String getDescription() { return description; }
}
