package com.globant.globerbooks.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps exceptions to the error envelope through the shared factory. The body is always JSON, even
 * when the client asks for another type. No message of a framework exception is echoed.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType JSON_UTF8 = MediaType.parseMediaType(ErrorResponseWriter.CONTENT_TYPE);

    private final ErrorBodyFactory factory;

    /**
     * Creates the advice.
     *
     * @param factory the body factory
     */
    public GlobalExceptionHandler(ErrorBodyFactory factory) {
        this.factory = factory;
    }

    /**
     * Maps an unknown path to {@code 404 NOT_FOUND}.
     *
     * @return the envelope
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorBody> handleNotFound() {
        return respond(ErrorCode.NOT_FOUND);
    }

    /**
     * Maps a wrong method to {@code 405 METHOD_NOT_ALLOWED}.
     *
     * @return the envelope
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorBody> handleMethod() {
        return respond(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * Maps an unacceptable {@code Accept} to {@code 406 NOT_ACCEPTABLE}.
     *
     * @return the envelope
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorBody> handleNotAcceptable() {
        return respond(ErrorCode.NOT_ACCEPTABLE);
    }

    /**
     * Maps an unsupported {@code Content-Type} to {@code 415 UNSUPPORTED_MEDIA_TYPE}.
     *
     * @return the envelope
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorBody> handleUnsupported() {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Maps any other failure to {@code 500 INTERNAL_ERROR}. Logs one line with the exception type and
     * its top frame, never its message.
     *
     * @param failure the failure
     * @return the envelope
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception failure) {
        StackTraceElement[] trace = failure.getStackTrace();
        String where = trace.length > 0 ? trace[0].toString() : "unknown";
        LOG.error("Unexpected error: {} at {}", failure.getClass().getName(), where);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ErrorBody> respond(ErrorCode code) {
        return ResponseEntity.status(code.status())
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8.toString())
                .body(factory.create(code));
    }
}
