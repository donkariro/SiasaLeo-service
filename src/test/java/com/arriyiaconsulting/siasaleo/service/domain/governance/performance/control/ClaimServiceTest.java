package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.CreateClaimRevisionRequest;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.AchievementClaim;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.ClaimRevision;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.ClaimRevisionRepository;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.DatePrecision;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Spy
    private PerformanceMapper mapper = Mappers.getMapper(PerformanceMapper.class);

    @Mock
    private ClaimRevisionRepository revisions;

    @Mock
    private NativeQueries locks;

    @InjectMocks
    private ClaimService service;

    @Test
    void publicReadsHideDrafts() {
        claim(9L, PublicationStatus.DRAFT);
        assertThrows(RecordNotFoundException.class, () -> service.readClaim(9L, false));
    }

    @Test
    void editorsReadDrafts() {
        claim(9L, PublicationStatus.DRAFT);
        assertEquals(PublicationStatus.DRAFT, service.readClaim(9L, true).publicationStatus());
    }

    @Test
    void publicReadsShowPublishedAndWithdrawnClaims() {
        claim(9L, PublicationStatus.PUBLISHED);
        claim(10L, PublicationStatus.WITHDRAWN);
        assertEquals(PublicationStatus.PUBLISHED, service.readClaim(9L, false).publicationStatus());
        assertEquals(PublicationStatus.WITHDRAWN, service.readClaim(10L, false).publicationStatus());
    }

    @Test
    void trackRecordPaginationIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> service.trackRecord(1, -1, 25));
        assertThrows(IllegalArgumentException.class, () -> service.trackRecord(1, 0, 101));
    }

    @Test
    void revisionAllocationLocksExistingClaimBeforeReadingCounter() throws Exception {
        AchievementClaim existing = new AchievementClaim();
        var idField = AchievementClaim.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existing, 7L);
        existing.setOfficeholderId(12L);
        when(locks.lock(AchievementClaim.class, 7L)).thenReturn(Optional.of(existing));
        when(revisions.latestNumber(7L)).thenReturn(1);
        when(revisions.insert(any())).then(returnsFirstArg());

        var request = new CreateClaimRevisionRequest(7L, 12L, "Built clinic", "Completed",
                3L, null, 4L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1),
                DatePrecision.DAY, "Report 1");
        var result = service.createClaim(request, 5L);

        assertEquals(2, result.revisionNumber());
        InOrder order = inOrder(locks, revisions);
        order.verify(locks).lock(AchievementClaim.class, 7L);
        order.verify(revisions).latestNumber(7L);
    }

    private void claim(long id, PublicationStatus status) {
        ClaimRevision revision = new ClaimRevision();
        revision.setPublicationStatus(status);
        when(revisions.findById(id)).thenReturn(Optional.of(revision));
    }
}
