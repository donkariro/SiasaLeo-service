package com.arriyiaconsulting.siasaleo.service.domain.result.dto;

import java.util.Arrays;
import java.util.Optional;

/**
 * Provisional results are the ones announced as tallies arrive; official ones
 * are the declared results. Mirrors the CHECK constraint on the stage columns
 * in V45.
 */
public enum ResultStage {
    OFFICIAL, PROVISIONAL;

    /** Requests and query parameters still carry the stage as text; empty when unrecognised. */
    public static Optional<ResultStage> parse(String value) {
        return Arrays.stream(values()).filter(s -> s.name().equals(value)).findFirst();
    }
}
