package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

/**
 * A person's gender as the electoral domain records it (V41). Constrained to
 * the two values Kenyan electoral law is written in — the Woman Representative
 * seat, the two-thirds gender rule, and IEBC's turnout reporting all turn on
 * this distinction. A person with no value recorded is null rather than a
 * third constant, so "unknown" never reads as an answer.
 */
public enum Gender {
    MALE,
    FEMALE
}
