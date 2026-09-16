package com.arriyiaconsulting.siasaleo.service.domain.education.entity;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Organization;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationName;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationType;
import jakarta.persistence.*;

@Entity
@Table(name = "educational_institution")
@DiscriminatorValue("EDUCATIONAL_INSTITUTION")
public class EducationalInstitution extends Organization {
    @ManyToOne(optional = false)
    @JoinColumn(name = "institution_type")
    private EducationalInstitutionType institutionType;

    protected EducationalInstitution() {}

    public EducationalInstitution(OrganizationName name, OrganizationType orgType,
                                  String registrationNumber, EducationalInstitutionType institutionType) {
        super(name, orgType, registrationNumber);
        this.institutionType = institutionType;
    }

    public EducationalInstitutionType getInstitutionType() { return institutionType; }
}
