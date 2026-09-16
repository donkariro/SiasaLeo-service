package com.arriyiaconsulting.siasaleo.service.domain.education.entity;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "person_education")
public class PersonEducation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(optional = false)
    @JoinColumn(name = "education_level")
    private EducationLevel educationLevel;

    @ManyToOne
    @JoinColumn(name = "educational_institution")
    private EducationalInstitution institution;

    @ManyToOne
    @JoinColumn(name = "field_of_study")
    private FieldOfStudy fieldOfStudy;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "upto_date")
    private LocalDate uptoDate;

    protected PersonEducation() {}

    public PersonEducation(Person person, EducationLevel level, EducationalInstitution institution,
                           FieldOfStudy field, LocalDate fromDate, LocalDate uptoDate) {
        this.person = person;
        revise(level, institution, field, fromDate, uptoDate);
    }

    public void revise(EducationLevel level, EducationalInstitution institution,
                       FieldOfStudy field, LocalDate fromDate, LocalDate uptoDate) {
        if (level == null) {
            throw new IllegalArgumentException("Education level is required");
        }
        if (fromDate != null && uptoDate != null && uptoDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Study end date must not precede start date");
        }
        this.educationLevel = level;
        this.institution = institution;
        this.fieldOfStudy = field;
        this.fromDate = fromDate;
        this.uptoDate = uptoDate;
    }

    public Long getId() { return id; }
    public Person getPerson() { return person; }
    public EducationLevel getEducationLevel() { return educationLevel; }
    public EducationalInstitution getInstitution() { return institution; }
    public FieldOfStudy getFieldOfStudy() { return fieldOfStudy; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getUptoDate() { return uptoDate; }
}
