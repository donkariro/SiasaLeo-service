package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing;

/**
 * Lifecycle of a sourced import (result publication, voter register): facts
 * are loaded into a DRAFT, and PUBLISHED records are immutable. Mirrors the
 * CHECK constraint on the status columns in V45, which stays the guarantee.
 */
public enum PublicationStatus {
    DRAFT, PUBLISHED
}
