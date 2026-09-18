package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

/** Also used for drafts hidden from public callers and cross-snapshot lookups. */
public class GeographyNotFoundException extends RuntimeException {
    public GeographyNotFoundException() { super("Geography snapshot or area not found"); }
}
