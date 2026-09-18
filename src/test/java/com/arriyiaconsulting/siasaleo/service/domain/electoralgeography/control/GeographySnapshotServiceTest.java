package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping.GeographySnapshotMapper;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeographySnapshotServiceTest {
    @Mock GeographySnapshotRepository snapshots;
    @Spy GeographySnapshotMapper mapper = Mappers.getMapper(GeographySnapshotMapper.class);
    @InjectMocks GeographySnapshotService service;

    private ElectoralGeographySnapshot draft() {
        return new ElectoralGeographySnapshot("Historical geography", null, "archive/register.pdf", "a".repeat(64), null);
    }

    @Test void publicCannotReadDraftButAdministratorCan() {
        var draft = draft();
        when(snapshots.find(1L)).thenReturn(Optional.of(draft));
        assertThrows(GeographyNotFoundException.class, () -> service.find(1L, false));
        assertEquals(SnapshotStatus.DRAFT, service.find(1L, true).status());
    }

    @Test void publishedSnapshotIsPublicAndCannotBeRevisedInPlace() {
        var published = draft();
        published.publish();
        when(snapshots.find(1L)).thenReturn(Optional.of(published));
        when(snapshots.lock(1L)).thenReturn(Optional.of(published));
        assertEquals(SnapshotStatus.PUBLISHED, service.find(1L, false).status());
        assertThrows(IllegalArgumentException.class, () -> service.reviseMetadata(1L,
                new CreateGeographySnapshotRequest("Changed", null, null, null)));
        assertEquals("Historical geography", published.getName());
    }

    @Test void publicationRequiresProvenanceAndCannotBeRepeated() {
        var draft = new ElectoralGeographySnapshot("Unverified", null, null, null, null);
        assertThrows(IllegalArgumentException.class, draft::publish);
        assertEquals(SnapshotStatus.DRAFT, draft.getStatus());
        draft.revise("Verified", null, "archive/source", "b".repeat(64));
        draft.publish();
        assertNotNull(draft.getPublishedAt());
        assertThrows(IllegalArgumentException.class, draft::publish);
    }

    @Test void revisionCopiesPublishedHierarchyWithoutChangingOriginal() {
        var original = draft();
        original.publish();
        when(snapshots.find(1L)).thenReturn(Optional.of(original));
        when(snapshots.insert(any())).thenAnswer(call -> {
            ElectoralGeographySnapshot value = call.getArgument(0);
            var field = ElectoralGeographySnapshot.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(value, 2L);
            return value;
        });
        var revision = service.createRevision(1L, new CreateGeographySnapshotRequest("Correction", null, null, null));
        assertEquals(1L, revision.supersedesSnapshotId());
        assertEquals(SnapshotStatus.DRAFT, revision.status());
        assertNull(revision.sourceReference());
        verify(snapshots).copyAreas(1L, 2L);
        assertEquals(SnapshotStatus.PUBLISHED, original.getStatus());
    }

    @Test void draftCannotBeRevisionSource() {
        when(snapshots.find(1L)).thenReturn(Optional.of(draft()));
        assertThrows(IllegalArgumentException.class, () -> service.createRevision(1L,
                new CreateGeographySnapshotRequest("Revision", null, null, null)));
        verify(snapshots, never()).insert(any());
    }

    @Test void listingFiltersDraftsAndBoundsPagination() {
        when(snapshots.list(false, 500, 500)).thenReturn(List.of());
        assertTrue(service.list(false, 1, 1000).isEmpty());
        verify(snapshots).list(false, 500, 500);
        assertThrows(IllegalArgumentException.class, () -> service.list(false, -1, 100));
        assertThrows(IllegalArgumentException.class, () -> service.list(false, Integer.MAX_VALUE, 100));
    }
}
