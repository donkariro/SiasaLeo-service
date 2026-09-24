package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks the whole transition table, every pair of statuses, so a change to
 * the lifecycle has to be made here on purpose.
 */
class ElectionStatusNameTest {

    private static final Set<String> ALLOWED = Set.of(
            "SCHEDULED>ONGOING", "SCHEDULED>CANCELLED",
            "ONGOING>COMPLETED", "ONGOING>NULLIFIED",
            "COMPLETED>NULLIFIED");

    @Test
    void transitionTableIsExactlyTheDocumentedLifecycle() {
        for (ElectionStatusName from : ElectionStatusName.values()) {
            for (ElectionStatusName to : ElectionStatusName.values()) {
                assertEquals(ALLOWED.contains(from + ">" + to), from.canChangeTo(to), from + " -> " + to);
            }
        }
    }

    @Test
    void statusSeededWithoutAConstantIsADeploymentError() {
        assertThrows(IllegalStateException.class, () -> ElectionStatusName.of("POSTPONED"));
        assertThrows(IllegalStateException.class, () -> ElectionStatusName.of(null));
    }

    @Test
    void eventOnlyReportsAChangeWhenOneHappens() {
        ElectionStatus scheduled = status("SCHEDULED");
        ElectionStatus ongoing = status("ONGOING");
        ElectionEvent event = new ElectionEvent(mock(ElectionCycle.class),
                LocalDate.of(2027, 8, 10), mock(ElectionType.class), scheduled);

        assertFalse(event.changeStatus(scheduled));
        assertTrue(event.changeStatus(ongoing));
        assertSame(ongoing, event.getStatus());
        assertThrows(IllegalArgumentException.class, () -> event.changeStatus(scheduled));
        assertSame(ongoing, event.getStatus());
    }

    private static ElectionStatus status(String name) {
        ElectionStatus status = mock(ElectionStatus.class);
        when(status.getStatusName()).thenReturn(name);
        return status;
    }
}
