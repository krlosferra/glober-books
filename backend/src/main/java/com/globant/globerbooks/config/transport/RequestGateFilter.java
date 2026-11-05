package com.globant.globerbooks.config.transport;

import com.globant.globerbooks.error.ErrorBody;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The first filter of the chain (AP-2 stages 1 to 6): sets the security headers, then checks Host,
 * Origin, Sec-Fetch-Site, Content-Type, the 16 KiB body cap, the route and method from the raw
 * target (with {@code Allow} on a 405) and {@code Accept} on API paths, and ends the request with
 * the error envelope on the first failure. It never reads a parameter, and a failure of any step or of
 * the rest of the chain becomes {@code INTERNAL_ERROR}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestGateFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestGateFilter.class);

    private static final String API_PREFIX = "/api/v1/";
    private static final String BOOKS_PREFIX = RouteTable.BOOKS + "/";
    private static final int ID_SEGMENT = 3;

    private final ErrorResponseWriter writer;
    private final Rejecter rejecter;
    private final DetailRejecter detailRejecter;
    private final RouteTable routes;

    /**
     * Creates the filter.
     *
     * @param writer the error writer
     * @param routes the route and method table
     */
    public RequestGateFilter(ErrorResponseWriter writer, RouteTable routes) {
        this.writer = writer;
        this.rejecter = writer::write;
        this.detailRejecter = writer::write;
        this.routes = routes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        EnvelopeResponseWrapper guarded = new EnvelopeResponseWrapper(response, writer);
        try {
            gate(request, guarded, chain);
        } catch (IOException | ServletException | RuntimeException | Error failure) {
            LOG.error("Unexpected error: {} at {}", failure.getClass().getName(), topFrame(failure));
            reject(guarded, ErrorCode.INTERNAL_ERROR);
        }
    }

    /** The whole gate (SEC-F-113): header checks, body buffering, route and request rules, then the chain. */
    private void gate(HttpServletRequest request, HttpServletResponse guarded, FilterChain chain)
            throws ServletException, IOException {
        ErrorCode refusal = checkHeaders(request);
        HttpServletRequest forwarded = request;
        if (refusal == null) {
            refusal = declaredBodyRefusal(request);
        }
        if (refusal == null) {
            try {
                forwarded = bufferedIfBody(request);
            } catch (BoundedBodyRequest.TooLargeException tooLarge) {
                refusal = ErrorCode.PAYLOAD_TOO_LARGE;
            } catch (BoundedBodyRequest.TooSlowException tooSlow) {
                refusal = ErrorCode.MALFORMED_REQUEST;
                guarded.setHeader("Connection", "close");
            }
        }
        if (refusal == null) {
            refusal = routeRefusal(request, guarded);
        }
        if (refusal == null) {
            refusal = requestRules(request, forwarded);
        }
        if (refusal != null) {
            reject(guarded, refusal);
            return;
        }
        chain.doFilter(forwarded, guarded);
    }

    private ErrorCode checkHeaders(HttpServletRequest request) {
        int port = request.getLocalPort();
        if (!HeaderPolicy.hostAllowed(values(request, "Host"), port)) {
            return ErrorCode.INVALID_HOST;
        }
        if (!HeaderPolicy.originAllowed(values(request, "Origin"), port)
                || !HeaderPolicy.fetchSiteAllowed(values(request, "Sec-Fetch-Site"))) {
            return ErrorCode.FORBIDDEN;
        }
        if (HeaderPolicy.requiresContentType(request.getMethod())
                && !HeaderPolicy.contentTypeAllowed(values(request, "Content-Type"))) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        return null;
    }

    private ErrorCode routeRefusal(HttpServletRequest request, HttpServletResponse response) {
        RequestTarget target = RequestTarget.of(request.getRequestURI());
        RouteTable.Decision decision = routes.decide(target, request.getMethod());
        switch (decision.outcome()) {
            case NOT_FOUND:
                return ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED:
                response.setHeader("Allow", decision.allow());
                return ErrorCode.METHOD_NOT_ALLOWED;
            default:
                break;
        }
        if (target.path().startsWith(API_PREFIX) && !AcceptPolicy.acceptable(values(request, "Accept"))) {
            return ErrorCode.NOT_ACCEPTABLE;
        }
        return null;
    }

    /** Stage 9 (AP-2): id as received, query grammar and status value, then the no-body rule. */
    private ErrorCode requestRules(HttpServletRequest request, HttpServletRequest forwarded) {
        RequestTarget target = RequestTarget.of(request.getRequestURI());
        List<String> segments = target.segments();
        if (target.path().startsWith(BOOKS_PREFIX) && segments.size() > ID_SEGMENT
                && !QueryPolicy.idWellFormed(segments.get(ID_SEGMENT))) {
            return ErrorCode.MALFORMED_REQUEST;
        }
        boolean list = "GET".equals(request.getMethod()) && RouteTable.BOOKS.equals(target.path());
        QueryPolicy.Verdict verdict = QueryPolicy.check(request.getQueryString(), list);
        switch (verdict.outcome()) {
            case MALFORMED:
                return ErrorCode.MALFORMED_REQUEST;
            case INVALID_STATUS:
                return ErrorCode.INVALID_STATUS;
            default:
                verdict.status().ifPresent(value -> request.setAttribute(QueryPolicy.STATUS_ATTRIBUTE, value));
        }
        boolean noBodyMethod = "GET".equals(request.getMethod()) || "DELETE".equals(request.getMethod());
        return noBodyMethod && forwarded.getContentLengthLong() > 0 ? ErrorCode.MALFORMED_REQUEST : null;
    }

    private ErrorCode declaredBodyRefusal(HttpServletRequest request) {
        return request.getContentLengthLong() > BoundedBodyRequest.LIMIT ? ErrorCode.PAYLOAD_TOO_LARGE : null;
    }

    private HttpServletRequest bufferedIfBody(HttpServletRequest request) throws IOException {
        boolean hasBody = request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
        return hasBody ? BoundedBodyRequest.read(request) : request;
    }

    private void reject(HttpServletResponse response, ErrorCode code) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        if (code == ErrorCode.PAYLOAD_TOO_LARGE) {
            response.setHeader("Connection", "close");
        }
        try {
            if (code == ErrorCode.INVALID_STATUS) {
                detailRejecter.write(response, code, List.of(new ErrorBody.FieldProblem("status",
                        "must be one of: unread, reading, completed")));
            } else {
                rejecter.write(response, code);
            }
        } catch (IllegalStateException unusable) {
            LOG.error("Error response could not be written: {}", unusable.getClass().getName());
        }
    }

    private static String topFrame(Throwable failure) {
        StackTraceElement[] trace = failure.getStackTrace();
        return trace.length > 0 ? trace[0].toString() : "unknown";
    }

    private static List<String> values(HttpServletRequest request, String name) {
        return Collections.list(request.getHeaders(name));
    }

    /** Writes the envelope of an error code to a response. */
    @FunctionalInterface
    private interface Rejecter {
        void write(HttpServletResponse response, ErrorCode code) throws IOException;
    }

    /** Writes the envelope of an error code with field problems. */
    @FunctionalInterface
    private interface DetailRejecter {
        void write(HttpServletResponse response, ErrorCode code, List<ErrorBody.FieldProblem> details)
                throws IOException;
    }
}
