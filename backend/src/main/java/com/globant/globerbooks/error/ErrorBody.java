package com.globant.globerbooks.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The error envelope of spec Appendix A: {@code error}, {@code code}, {@code timestamp} and,
 * only for the codes that define it, {@code details} as a list of field problems.
 *
 * @param error informative fixed text
 * @param code the error code name
 * @param timestamp UTC timestamp, format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}
 * @param details field problems, or {@code null} when the code has no details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorBody(String error, String code, String timestamp, List<FieldProblem> details) {

    /**
     * Copies the details list so the record is immutable.
     */
    public ErrorBody {
        details = details == null ? null : List.copyOf(details);
    }

    /**
     * One invalid field.
     *
     * @param field the API field name ({@code title}, {@code author}, {@code status}, {@code rating})
     * @param message a fixed detail message
     */
    public record FieldProblem(String field, String message) {
    }
}
