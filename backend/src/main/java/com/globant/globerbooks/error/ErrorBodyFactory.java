package com.globant.globerbooks.error;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The single place that builds error bodies. The timestamp comes from the injected clock.
 */
@Component
public class ErrorBodyFactory {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC);

    private final Clock clock;

    /**
     * Creates the factory.
     *
     * @param clock the application clock
     */
    public ErrorBodyFactory(Clock clock) {
        this.clock = clock;
    }

    /**
     * Builds a body without details.
     *
     * @param code the error code
     * @return the envelope
     */
    public ErrorBody create(ErrorCode code) {
        return new ErrorBody(code.message(), code.name(), now(), null);
    }

    /**
     * Builds a body with field problems.
     *
     * @param code the error code
     * @param details the field problems (must not be empty)
     * @return the envelope
     */
    public ErrorBody create(ErrorCode code, List<ErrorBody.FieldProblem> details) {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("details must not be empty");
        }
        return new ErrorBody(code.message(), code.name(), now(), details);
    }

    private String now() {
        return TIMESTAMP.format(clock.instant());
    }
}
