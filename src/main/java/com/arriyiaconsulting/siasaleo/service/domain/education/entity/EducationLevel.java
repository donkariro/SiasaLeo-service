package com.arriyiaconsulting.siasaleo.service.domain.education.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "education_level")
public class EducationLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_name", nullable = false, unique = true, length = 50)
    private String levelName;

    @Column(length = 255)
    private String description;

    @Column(name = "level_order", unique = true)
    private Integer levelOrder;

    public Integer getLevelOrder() { return levelOrder; }

    protected EducationLevel() {}

    public Long getId() { return id; }
    public String getLevelName() { return levelName; }
    public String getDescription() { return description; }
}
