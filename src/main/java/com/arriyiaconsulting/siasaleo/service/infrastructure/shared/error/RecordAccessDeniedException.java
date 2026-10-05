package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

public class RecordAccessDeniedException extends RuntimeException {
    public RecordAccessDeniedException() { super("The active account does not own this record"); }
}
