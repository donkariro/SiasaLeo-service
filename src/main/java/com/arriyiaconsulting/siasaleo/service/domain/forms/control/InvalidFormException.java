package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormProblem;
import java.util.List;

/** A form definition or answer document failed validation; carries every problem found, not just the first. */
public class InvalidFormException extends RuntimeException {
    private final List<FormProblem> problems;
    public InvalidFormException(String message, List<FormProblem> problems) {
        super(message);
        this.problems = List.copyOf(problems);
    }
    public List<FormProblem> getProblems() { return problems; }
}
