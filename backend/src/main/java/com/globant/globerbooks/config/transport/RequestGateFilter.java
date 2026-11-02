package com.globant.globerbooks.config.transport;

import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The first filter of the chain (AP-2 stages 1 to 5): sets the security headers, then checks Host,
 * Origin, Sec-Fetch-Site, Content-Type and the 16 KiB body cap, and ends the request with the
 * error envelope on the first failure. It never reads a parameter, and a failure of the rest of
 * the chain becomes {@code INTERNAL_ERROR}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestGateFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestGateFilter.class);

    private final Rejecter rejecter;

    /**
     * Creates the filter.
     *
     * @param writer the error writer
     */
    public RequestGateFilter(ErrorResponseWriter writer) {
        this.rejecter = writer::write;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletResponse guarded = new HeaderResponse(response);
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
            }
        }
        if (refusal != null) {
            reject(guarded, refusal);
            return;
        }
        proceed(forwarded, guarded, chain);
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

    private ErrorCode declaredBodyRefusal(HttpServletRequest request) {
        return request.getContentLengthLong() > BoundedBodyRequest.LIMIT ? ErrorCode.PAYLOAD_TOO_LARGE : null;
    }

    private HttpServletRequest bufferedIfBody(HttpServletRequest request) throws IOException {
        boolean hasBody = request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
        return hasBody ? BoundedBodyRequest.read(request) : request;
    }

    private void proceed(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error failure) {
            LOG.error("Unexpected error: {} at {}", failure.getClass().getName(), topFrame(failure));
            reject(response, ErrorCode.INTERNAL_ERROR);
        }
    }

    private void reject(HttpServletResponse response, ErrorCode code) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        if (code == ErrorCode.PAYLOAD_TOO_LARGE) {
            response.setHeader("Connection", "close");
        }
        try {
            rejecter.write(response, code);
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

    /** Response that sets the security headers again after {@code reset} and before {@code sendError}. */
    private static final class HeaderResponse extends HttpServletResponseWrapper {

        HeaderResponse(HttpServletResponse response) {
            super(response);
            apply();
        }

        private void apply() {
            for (Map.Entry<String, String> header : HeaderPolicy.SECURITY_HEADERS.entrySet()) {
                super.setHeader(header.getKey(), header.getValue());
            }
        }

        @Override
        public void reset() {
            super.reset();
            apply();
        }

        @Override
        public void sendError(int status) throws IOException {
            apply();
            super.sendError(status);
        }

        @Override
        public void sendError(int status, String message) throws IOException {
            apply();
            super.sendError(status, message);
        }
    }
}
