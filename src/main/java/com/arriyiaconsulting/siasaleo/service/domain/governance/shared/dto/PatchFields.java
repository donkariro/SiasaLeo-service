package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import java.util.HashSet;
import java.util.Set;

/** Presence tracking belongs to HTTP DTOs, never to persistence. */
public abstract class PatchFields {
    @JsonbTransient private final Set<String> supplied = new HashSet<>();
    protected void mark(String property) { supplied.add(property); }
    public boolean has(String property) { return supplied.contains(property); }
    public void requireChanges() {
        if (supplied.isEmpty()) throw new IllegalArgumentException("At least one editable property is required");
    }
}
