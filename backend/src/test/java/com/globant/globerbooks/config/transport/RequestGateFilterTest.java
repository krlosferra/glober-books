package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.globant.globerbooks.error.ErrorBodyFactory;
import com.globant.globerbooks.error.ErrorResponseWriter;

/** Unit tests of the gate (tasks.md Task 06 criteria 1 to 4; AP-2 stages 1 to 5). */
class RequestGateFilterTest {

    private static final int PORT = 18080;
    private static final String JSON = "application/json";

    private final RequestGateFilter filter = new RequestGateFilter(new ErrorResponseWriter(
            new ErrorBodyFactory(Clock.fixed(Instant.parse("2026-02-19T10:00:00.123Z"), ZoneOffset.UTC))));
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicBoolean reached = new AtomicBoolean();
    private final AtomicReference<HttpServletRequest> seen = new AtomicReference<>();

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/books");
        request.setLocalPort(PORT);
        request.addHeader("Host", "localhost:" + PORT);
        return request;
    }

    private MockHttpServletRequest post(byte[] body) {
        MockHttpServletRequest request = request("POST");
        request.setContentType(JSON);
        request.setContent(body);
        return request;
    }

    private void run(HttpServletRequest request) throws Exception {
        FilterChain chain = (req, res) -> {
            reached.set(true);
            seen.set((HttpServletRequest) req);
        };
        filter.doFilter(request, response, chain);
    }

    private void assertRejected(int status, String code) throws Exception {
        assertThat(reached).isFalse();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).isEqualTo("application/json; charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":\"" + code + "\"");
        HeaderPolicy.SECURITY_HEADERS.forEach((name, value) -> assertThat(response.getHeader(name)).isEqualTo(value));
    }

    @Test
    void passesAnAllowedGetWithSecurityHeaders() throws Exception {
        run(request("GET"));

        assertThat(reached).isTrue();
        HeaderPolicy.SECURITY_HEADERS.forEach((name, value) -> assertThat(response.getHeader(name)).isEqualTo(value));
    }

    @Test
    void forgedHostGives400InvalidHost() throws Exception {
        MockHttpServletRequest request = request("GET");
        request.removeHeader("Host");
        request.addHeader("Host", "evil.example");

        run(request);

        assertRejected(400, "INVALID_HOST");
    }

    @Test
    void badOriginGives403Forbidden() throws Exception {
        MockHttpServletRequest request = request("GET");
        request.addHeader("Origin", "http://evil.example");

        run(request);

        assertRejected(403, "FORBIDDEN");
    }

    @Test
    void preflightWithForeignOriginGives403WithoutCorsHeaders() throws Exception {
        MockHttpServletRequest request = request("OPTIONS");
        request.addHeader("Origin", "http://evil.example");
        request.addHeader("Access-Control-Request-Method", "POST");

        run(request);

        assertRejected(403, "FORBIDDEN");
        assertThat(response.getHeaderNames()).noneMatch(name -> name.startsWith("Access-Control"));
    }

    @Test
    void ownOriginPasses() throws Exception {
        MockHttpServletRequest request = request("GET");
        request.addHeader("Origin", "http://127.0.0.1:" + PORT);
        request.addHeader("Sec-Fetch-Site", "same-origin");

        run(request);

        assertThat(reached).isTrue();
    }

    @Test
    void crossSiteFetchMetadataGives403Forbidden() throws Exception {
        MockHttpServletRequest request = request("GET");
        request.addHeader("Sec-Fetch-Site", "cross-site");

        run(request);

        assertRejected(403, "FORBIDDEN");
    }

    @Test
    void hostIsJudgedBeforeOrigin() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.removeHeader("Host");
        request.addHeader("Host", "evil.example");
        request.addHeader("Origin", "http://evil.example");
        run(request);

        assertRejected(400, "INVALID_HOST");
    }

    @Test
    void originIsJudgedBeforeContentType() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.addHeader("Origin", "http://evil.example");

        run(request);

        assertRejected(403, "FORBIDDEN");
    }

    @Test
    void postPutDeleteWithoutContentTypeGive415() throws Exception {
        for (String method : new String[] {"POST", "PUT", "DELETE"}) {
            MockHttpServletResponse fresh = new MockHttpServletResponse();
            filter.doFilter(request(method), fresh, (req, res) -> reached.set(true));
            assertThat(reached).isFalse();
            assertThat(fresh.getStatus()).as(method).isEqualTo(415);
            assertThat(fresh.getContentAsString()).contains("UNSUPPORTED_MEDIA_TYPE");
        }
    }

    @Test
    void wrongContentTypeGives415() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.setContentType("text/plain");

        run(request);

        assertRejected(415, "UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void getWithoutContentTypePasses() throws Exception {
        run(request("GET"));

        assertThat(reached).isTrue();
    }

    @Test
    void declaredOverLimitBodyGives413WithoutReadingAnyByte() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        MockHttpServletRequest inner = post(new byte[16_385]);
        inner.addHeader("Content-Length", "16385");
        HttpServletRequest counting = new HttpServletRequestWrapper(inner) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                reads.incrementAndGet();
                return super.getInputStream();
            }
        };

        run(counting);

        assertRejected(413, "PAYLOAD_TOO_LARGE");
        assertThat(reads).hasValue(0);
        assertThat(response.getHeader("Connection")).isEqualTo("close");
    }

    @Test
    void declaredBodyOfExactlyTheLimitPassesAndIsReadable() throws Exception {
        byte[] body = new byte[16_384];
        java.util.Arrays.fill(body, (byte) ' ');
        MockHttpServletRequest request = post(body);
        request.addHeader("Content-Length", "16384");

        run(request);

        assertThat(reached).isTrue();
        assertThat(seen.get().getInputStream().readAllBytes()).hasSize(16_384);
    }

    @Test
    void chunkedBodyOverLimitGives413AtByte16385() throws Exception {
        MockHttpServletRequest request = post(new byte[16_385]);
        request.addHeader("Transfer-Encoding", "chunked");

        run(request);

        assertRejected(413, "PAYLOAD_TOO_LARGE");
        assertThat(response.getHeader("Connection")).isEqualTo("close");
    }

    @Test
    void chunkedBodyAtTheLimitPassesAndIsReplayed() throws Exception {
        MockHttpServletRequest request = post(new byte[16_384]);
        request.addHeader("Transfer-Encoding", "chunked");

        run(request);

        assertThat(reached).isTrue();
        assertThat(seen.get().getInputStream().readAllBytes()).hasSize(16_384);
    }

    @Test
    void neverReadsParameters() throws Exception {
        HttpServletRequest spied = spy(post("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        run(spied);

        assertThat(reached).isTrue();
        verify(spied, never()).getParameter(org.mockito.ArgumentMatchers.anyString());
        verify(spied, never()).getParameterMap();
        verify(spied, never()).getParameterNames();
        verify(spied, never()).getParameterValues(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void unexpectedExceptionGives500InternalErrorWithoutUserText() throws Exception {
        filter.doFilter(request("GET"), response, (req, res) -> {
            throw new IllegalStateException("secret user text");
        });

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("\"code\":\"INTERNAL_ERROR\"")
                .contains("Unexpected error.").doesNotContain("secret");
        HeaderPolicy.SECURITY_HEADERS.forEach((name, value) -> assertThat(response.getHeader(name)).isEqualTo(value));
    }

    @Test
    void failureAfterWriterWasOpenedStillAnswersJson() throws Exception {
        filter.doFilter(request("GET"), response, (req, res) -> {
            res.getOutputStream();
            throw new IllegalStateException("boom");
        });

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("INTERNAL_ERROR");
    }

    @Test
    void committedResponseIsLeftAloneWhenTheChainFails() throws Exception {
        HttpServletResponse committed = mock(HttpServletResponse.class);
        doReturn(true).when(committed).isCommitted();

        filter.doFilter(request("GET"), committed, (req, res) -> {
            throw new IllegalStateException("late failure");
        });

        verify(committed, never()).getOutputStream();
        verify(committed, never()).resetBuffer();
        verify(committed, never()).setStatus(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void securityHeadersSurviveResetAndSendError() throws Exception {
        filter.doFilter(request("GET"), response, (req, res) -> {
            HttpServletResponse wrapped = (HttpServletResponse) res;
            wrapped.reset();
            assertThat(wrapped.getHeader("X-Frame-Options")).isEqualTo("DENY");
            wrapped.reset();
            wrapped.sendError(404);
        });

        HeaderPolicy.SECURITY_HEADERS.forEach((name, value) -> assertThat(response.getHeader(name)).isEqualTo(value));
        assertThat(response.getStatus()).isEqualTo(404);
    }
}
