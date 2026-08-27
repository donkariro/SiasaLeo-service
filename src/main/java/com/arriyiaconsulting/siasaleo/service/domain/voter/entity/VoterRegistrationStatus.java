package com.arriyiaconsulting.siasaleo.service.domain.voter.entity;

/**
 * Lifecycle of a voter registration (V15). Only ACTIVE is constrained to one
 * row per person; TRANSFERRED and DEREGISTERED rows are retained as history.
 */
public enum VoterRegistrationStatus {
    ACTIVE,
    TRANSFERRED,
    DEREGISTERED
}
