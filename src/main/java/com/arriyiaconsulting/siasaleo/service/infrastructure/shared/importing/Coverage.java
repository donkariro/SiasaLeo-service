package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing;

import java.util.Arrays;
import java.util.Optional;

/**
 * Whether an import claims every area at its granularity (COMPLETE) or only
 * some (PARTIAL). Mirrors the CHECK constraint on the coverage columns in V45.
 */
public enum Coverage {
    COMPLETE, PARTIAL;

    /** Request bodies still carry coverage as text; empty when unrecognised. */
    public static Optional<Coverage> parse(String value) {
        return Arrays.stream(values()).filter(c -> c.name().equals(value)).findFirst();
    }
}
