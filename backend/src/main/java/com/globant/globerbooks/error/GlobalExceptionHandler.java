package com.globant.globerbooks.error;

import com.globant.globerbooks.service.BookValidationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    private static final Map<String, String> FIELD_MESSAGES = Map.of(
            "title", "must be between 1 and 500 characters",
            "author", "must be between 1 and 200 characters");

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
     * Maps a service validation failure to its code with the failing fields.
     *
     * @param failure the service exception
     * @return the envelope with {@code details}
     */
    @ExceptionHandler(BookValidationException.class)
    public ResponseEntity<ErrorBody> handleValidation(BookValidationException failure) {
        return respond(failure.code(), failure.problems());
    }

    /**
     * Maps a failed Bean Validation check of the body to {@code 400 VALIDATION_ERROR}. Messages
     * are fixed per field and never echo the rejected value.
     *
     * @param failure the binding failure
     * @return the envelope with {@code details}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleInvalidBody(MethodArgumentNotValidException failure) {
        List<ErrorBody.FieldProblem> problems = failure.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField())
                .distinct()
                .sorted()
                .map(field -> new ErrorBody.FieldProblem(field, FIELD_MESSAGES.getOrDefault(field,
                        "is invalid")))
                .toList();
        return respond(ErrorCode.VALIDATION_ERROR, problems);
    }

    /**
     * Maps an unreadable body (bad JSON, unknown member, wrong type) to {@code 400 MALFORMED_REQUEST}.
     *
     * @return the envelope
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleUnreadable() {
        return respond(ErrorCode.MALFORMED_REQUEST);
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

    private ResponseEntity<ErrorBody> respond(ErrorCode code, List<ErrorBody.FieldProblem> details) {
        return ResponseEntity.status(code.status())
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8.toString())
                .body(factory.create(code, details));
    }

    private ResponseEntity<ErrorBody> respond(ErrorCode code) {
        return ResponseEntity.status(code.status())
                .header(HttpHeaders.CONTENT_TYPE, JSON_UTF8.toString())
                .body(factory.create(code));
    }
}
