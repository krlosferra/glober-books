package com.globant.globerbooks.service;

import com.globant.globerbooks.error.ErrorBody;
import com.globant.globerbooks.error.ErrorCode;
import java.util.List;

/**
 * Thrown by the service when input breaks a field rule. It carries the error code and the field
 * problems; the exception advice turns it into the error envelope. Its message holds no user input.
 */
public class BookValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final transient List<ErrorBody.FieldProblem> problems;

    /**
     * Creates the exception.
     *
     * @param code {@code VALIDATION_ERROR} or {@code INVALID_STATUS}
     * @param problems the failing fields, never empty
     */
    public BookValidationException(ErrorCode code, List<ErrorBody.FieldProblem> problems) {
        super(code.name());
        this.code = code;
        this.problems = List.copyOf(problems);
    }

    /**
     * Returns the error code.
     *
     * @return the code
     */
    public ErrorCode code() {
        return code;
    }

    /**
     * Returns the field problems.
     *
     * @return an unmodifiable, non-empty list
     */
    public List<ErrorBody.FieldProblem> problems() {
        return problems;
    }
}
