package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

public class RecordConflictException extends RuntimeException {
    public RecordConflictException(String message) { super(message); }
    public RecordConflictException(Throwable cause) { super("Write conflicts with an existing record or publication constraint; validate the import and reload its current revision",cause); }
}
