package com.globant.globerbooks.error;

import com.globant.globerbooks.config.transport.RequestTarget;
import com.globant.globerbooks.config.transport.RouteTable;
import com.globant.globerbooks.service.BookValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final List<String> FIELD_ORDER = List.of("title", "author", "status");
    private static final Set<String> FIXED_MESSAGES = Set.of("is required", "must not be empty",
            "must be between 1 and 500 characters", "must be between 1 and 200 characters",
            "contains characters that are not allowed", "must be one of: unread, reading, completed");

    private final ErrorBodyFactory factory;
    private final RouteTable routes;

    /**
     * Creates the advice.
     *
     * @param factory the body factory
     * @param routes the route table, the one place that computes {@code Allow}
     */
    public GlobalExceptionHandler(ErrorBodyFactory factory, RouteTable routes) {
        this.factory = factory;
        this.routes = routes;
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
     * Maps a failed Bean Validation check of the body: one entry per failing field ordered title,
     * author, status; {@code INVALID_STATUS} when only {@code status} fails (BR-012 step 6), else
     * {@code VALIDATION_ERROR}. Only messages from the fixed list pass; none echoes the value.
     *
     * @param failure the binding failure
     * @return the envelope with {@code details}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleInvalidBody(MethodArgumentNotValidException failure) {
        Map<String, String> byField = new HashMap<>();
        failure.getBindingResult().getFieldErrors().forEach(error -> byField.putIfAbsent(error.getField(),
                fixedMessage(error.getDefaultMessage())));
        List<ErrorBody.FieldProblem> problems = byField.keySet().stream()
                .sorted(Comparator.comparingInt(GlobalExceptionHandler::position).thenComparing(field -> field))
                .map(field -> new ErrorBody.FieldProblem(field, byField.get(field)))
                .toList();
        boolean onlyStatus = problems.size() == 1 && "status".equals(problems.get(0).field());
        return respond(onlyStatus ? ErrorCode.INVALID_STATUS : ErrorCode.VALIDATION_ERROR, problems);
    }

    private static int position(String field) {
        int index = FIELD_ORDER.indexOf(field);
        return index < 0 ? FIELD_ORDER.size() : index;
    }

    private static String fixedMessage(String message) {
        return message != null && FIXED_MESSAGES.contains(message) ? message : "is invalid";
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
     * Maps a transport-rule violation (for example a query on a static path) to {@code 400 MALFORMED_REQUEST}.
     *
     * @return the envelope
     */
    @ExceptionHandler(MalformedRequestException.class)
    public ResponseEntity<ErrorBody> handleMalformed() {
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
     * Maps a wrong method to {@code 405 METHOD_NOT_ALLOWED} with the {@code Allow} of the route table.
     *
     * @param request the request, whose raw path selects the {@code Allow} value
     * @return the envelope
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorBody> handleMethod(HttpServletRequest request) {
        ResponseEntity<ErrorBody> answer = respond(ErrorCode.METHOD_NOT_ALLOWED);
        Optional<String> allow = routes.allowFor(RequestTarget.of(request.getRequestURI()));
        if (allow.isEmpty()) {
            return answer;
        }
        return ResponseEntity.status(answer.getStatusCode()).headers(answer.getHeaders())
                .header("Allow", allow.get()).body(answer.getBody());
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
