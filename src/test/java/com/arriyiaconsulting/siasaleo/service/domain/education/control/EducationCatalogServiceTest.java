package com.arriyiaconsulting.siasaleo.service.domain.education.control;

import org.mapstruct.factory.Mappers;
import com.arriyiaconsulting.siasaleo.service.domain.education.mapping.EducationMapper;
import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationType;
import jakarta.data.page.PageRequest;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EducationCatalogServiceTest {

    @Spy
    private EducationMapper educationMapper = Mappers.getMapper(EducationMapper.class);

    @Mock EducationLevelRepository levels;
    @Mock EducationalInstitutionTypeRepository types;
    @Mock FieldOfStudyRepository fields;
    @Mock EducationalInstitutionRepository institutions;
    @Mock EntityManager entityManager;
    @InjectMocks EducationCatalogService service;

    @Test
    void institutionSearchNormalizesTextAndBoundsPagination() {
        when(institutions.search(isNull(), eq("%university%"), any())).thenReturn(List.of());
        assertEquals(List.of(), service.institutions(null, " University ", 2, 900));
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        verify(institutions).search(isNull(), eq("%university%"), page.capture());
        assertEquals(3L, page.getValue().page());
        assertEquals(500, page.getValue().size());
        assertFalse(page.getValue().requestTotal());
    }

    @Test
    void negativePageAndUnknownTypeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.institutions(null, null, -1, 100));
        assertThrows(IllegalArgumentException.class, () -> service.institutions(99L, null, 0, 100));
        verifyNoInteractions(institutions);
    }

    @Test
    void professionalQualificationKeepsNullRank() {
        EducationLevel level = mock(EducationLevel.class);
        when(level.getId()).thenReturn(9L);
        when(level.getLevelName()).thenReturn("PROFESSIONAL_QUALIFICATION");
        when(level.getLevelOrder()).thenReturn(null);
        when(levels.findAllOrdered()).thenReturn(List.of(level));
        assertNull(service.levels().getFirst().levelOrder());
    }

    @Test
    void fieldsExposeParentForHierarchicalSelection() {
        FieldOfStudy parent = mock(FieldOfStudy.class);
        when(parent.getId()).thenReturn(1L);
        FieldOfStudy child = mock(FieldOfStudy.class);
        when(child.getId()).thenReturn(2L);
        when(child.getFieldName()).thenReturn("LAW");
        when(child.getParent()).thenReturn(parent);
        when(fields.findAllOrdered()).thenReturn(List.of(child));
        assertEquals(1L, service.fields().getFirst().parentId());
    }

    @Test
    void createsInstitutionWithEducationOrganizationClassification() {
        EducationalInstitutionType type = mock(EducationalInstitutionType.class);
        when(type.getId()).thenReturn(4L);
        when(type.getTypeName()).thenReturn("UNIVERSITY");
        when(types.findById(4L)).thenReturn(Optional.of(type));
        OrganizationType orgType = mock(OrganizationType.class);
        @SuppressWarnings("unchecked")
        TypedQuery<OrganizationType> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(OrganizationType.class))).thenReturn(query);
        when(query.setParameter("name", "EDUCATIONAL_INSTITUTION")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(orgType);
        when(institutions.save(any(EducationalInstitution.class))).thenAnswer(call -> call.getArgument(0));

        InstitutionDto result = service.createInstitution(new CreateInstitutionRequest(" University ", "A1", 4L));

        assertEquals("University", result.name());
        assertEquals("A1", result.registrationNumber());
        assertEquals(4L, result.institutionTypeId());
        ArgumentCaptor<EducationalInstitution> saved = ArgumentCaptor.forClass(EducationalInstitution.class);
        verify(institutions).save(saved.capture());
        assertSame(orgType, saved.getValue().getOrgType());
    }

    @Test
    void rejectsMissingInstitutionDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createInstitution(new CreateInstitutionRequest(" ", null, 4L)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createInstitution(new CreateInstitutionRequest("University", null, 99L)));
        verifyNoInteractions(institutions);
    }
}
