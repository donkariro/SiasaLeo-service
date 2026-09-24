package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error;

public class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException() { super("Record not found"); }
}
