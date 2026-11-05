package com.globant.globerbooks.config.transport;

import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Map;

/**
 * Response given to the rest of the chain (AP-1, AP-2 stage 1): it sets the security headers when it
 * is created and again after {@code reset}, and turns every {@code sendError} into the error envelope
 * written by the {@link ErrorResponseWriter}, so that a container error page is never produced inside
 * the application (SC-02, SC-11, INV-6). The message given to {@code sendError} is never written.
 */
public final class EnvelopeResponseWrapper extends HttpServletResponseWrapper {

    private final Rejecter rejecter;

    /**
     * Wraps a response and sets the security headers on it.
     *
     * @param response the container response
     * @param delegate writes the envelope of a code (the {@link ErrorResponseWriter})
     */
    public EnvelopeResponseWrapper(HttpServletResponse response, Rejecter delegate) {
        super(response);
        this.rejecter = delegate::write;
        applyHeaders();
    }

    /**
     * Maps a status to an error code: the statuses of the API keep their code, any other status
     * gives {@code INTERNAL_ERROR}.
     *
     * @param status the status given to {@code sendError}
     * @return the code of the envelope
     */
    static ErrorCode codeFor(int status) {
        return switch (status) {
            case 400 -> ErrorCode.MALFORMED_REQUEST;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    @Override
    public void reset() {
        super.reset();
        applyHeaders();
    }

    @Override
    public void sendError(int status) throws IOException {
        envelope(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        envelope(status);
    }

    private void envelope(int status) throws IOException {
        if (isCommitted()) {
            return;
        }
        super.resetBuffer();
        applyHeaders();
        rejecter.write((HttpServletResponse) getResponse(), codeFor(status));
    }

    private void applyHeaders() {
        for (Map.Entry<String, String> header : HeaderPolicy.SECURITY_HEADERS.entrySet()) {
            super.setHeader(header.getKey(), header.getValue());
        }
    }

    /** Writes the envelope of an error code to a response. */
    @FunctionalInterface
    public interface Rejecter {
        /**
         * Writes the envelope.
         *
         * @param response the response
         * @param code the error code
         * @throws IOException when the response cannot be written
         */
        void write(HttpServletResponse response, ErrorCode code) throws IOException;
    }
}
