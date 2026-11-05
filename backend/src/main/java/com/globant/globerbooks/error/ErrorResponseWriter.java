package com.globant.globerbooks.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an error envelope, and nothing else, to a servlet response. Used by filters and by
 * the response wrapper.
 */
@Component
public class ErrorResponseWriter {

    /** Explicit content type of every error body. */
    public static final String CONTENT_TYPE = "application/json; charset=UTF-8";

    private final ErrorBodyFactory factory;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    /**
     * Creates the writer.
     *
     * @param factory the body factory
     */
    public ErrorResponseWriter(ErrorBodyFactory factory) {
        this.factory = factory;
    }

    /**
     * Resets the response and writes the envelope for a code without details.
     *
     * @param response the response
     * @param code the error code
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        write(response, code, factory.create(code));
    }

    /**
     * Resets the response and writes the envelope for a code with field problems.
     *
     * @param response the response
     * @param code the error code
     * @param details the field problems (must not be empty)
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, ErrorCode code, List<ErrorBody.FieldProblem> details)
            throws IOException {
        write(response, code, factory.create(code, details));
    }

    /**
     * Resets the response and writes a prepared envelope with the status of the code.
     *
     * @param response the response
     * @param code the error code, which decides the status
     * @param body the envelope
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, ErrorCode code, ErrorBody body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        response.resetBuffer();
        response.setStatus(code.status());
        response.setContentType(CONTENT_TYPE);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }
}
