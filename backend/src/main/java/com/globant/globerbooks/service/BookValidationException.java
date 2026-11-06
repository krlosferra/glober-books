package com.globant.globerbooks.service;

import com.globant.globerbooks.error.ErrorBody;
import com.globant.globerbooks.error.ErrorCode;
import java.util.List;

/**
 * Thrown by the service when input breaks a field rule. It carries the error code and the field
 * problems; the exception advice turns it into the error envelope. Its message holds no user input.
 */
public class BookValidationException extends RuntimeException {

    /** Fixed detail message of a status that is not one of the three values. */
    public static final String STATUS_MESSAGE = "must be one of: unread, reading, completed";

    /** Fixed detail message of a rating that is not an integer from 1 to 5; it never holds the value. */
    public static final String RATING_MESSAGE = "must be an integer from 1 to 5";

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final transient List<ErrorBody.FieldProblem> problems;

    /**
     * Creates the exception.
     *
     * @param code {@code VALIDATION_ERROR}, {@code INVALID_STATUS} or {@code INVALID_RATING}
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

    /**
     * Returns the fixed detail message of a failed text check (spec Section 5.0 list). The text
     * never contains user input.
     *
     * @param problem the first failing check
     * @param max the limit in code points, used only for the length message
     * @return the message
     */
    public static String messageFor(TextPolicy.Problem problem, int max) {
        return switch (problem) {
            case REQUIRED -> "is required";
            case EMPTY -> "must not be empty";
            case TOO_LONG -> "must be between 1 and " + max + " characters";
            case FORBIDDEN_CHARACTERS -> "contains characters that are not allowed";
        };
    }
}
