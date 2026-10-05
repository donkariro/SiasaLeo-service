package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.CreateObservationRequest;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.Observation;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.ObservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationServiceTest {

    @Spy
    private PerformanceMapper mapper = Mappers.getMapper(PerformanceMapper.class);

    @Mock
    private ObservationRepository observations;

    @InjectMocks
    private ObservationService service;

    @Test
    void observationRecordsAuthenticatedActorWithoutSubjectAccount() {
        when(observations.insert(any())).then(returnsFirstArg());

        service.createObservation(new CreateObservationRequest(
                null, null, "Work documented", 3L, null, null, null, null, null, null), 7L);

        ArgumentCaptor<Observation> inserted = ArgumentCaptor.forClass(Observation.class);
        verify(observations).insert(inserted.capture());
        assertEquals(7L, inserted.getValue().getRecordedByAccountId());
        assertEquals(3L, inserted.getValue().getOfficeholderId());
        assertEquals("Work documented", inserted.getValue().getQualitativeResult());
    }
}
