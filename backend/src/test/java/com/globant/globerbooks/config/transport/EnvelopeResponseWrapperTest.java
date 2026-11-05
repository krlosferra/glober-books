package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.error.ErrorBodyFactory;
import com.globant.globerbooks.error.ErrorCode;
import com.globant.globerbooks.error.ErrorResponseWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests of the response wrapper (tasks.md Task 17 criteria 1 and 3; SC-02, SC-11, INV-6). */
class EnvelopeResponseWrapperTest {

    private final MockHttpServletResponse container = new MockHttpServletResponse();
    private final EnvelopeResponseWrapper wrapper = new EnvelopeResponseWrapper(container, new ErrorResponseWriter(
            new ErrorBodyFactory(Clock.fixed(Instant.parse("2026-02-19T10:00:00.123Z"), ZoneOffset.UTC)))::write);

    private void assertAllHeaders() {
        HeaderPolicy.SECURITY_HEADERS.forEach((name, value) -> assertThat(container.getHeader(name)).as(name)
                .isEqualTo(value));
    }

    @ParameterizedTest
    @CsvSource({"400,MALFORMED_REQUEST", "403,FORBIDDEN", "404,NOT_FOUND", "405,METHOD_NOT_ALLOWED",
        "406,NOT_ACCEPTABLE", "413,PAYLOAD_TOO_LARGE", "415,UNSUPPORTED_MEDIA_TYPE", "500,INTERNAL_ERROR",
        "401,INTERNAL_ERROR", "418,INTERNAL_ERROR", "503,INTERNAL_ERROR"})
    void sendErrorLeavesAsTheEnvelopeOfTheMappedCode(int status, ErrorCode code) throws IOException {
        wrapper.sendError(status);

        assertThat(container.getStatus()).isEqualTo(code.status());
        assertThat(container.getContentType()).isEqualTo("application/json; charset=UTF-8");
        assertThat(container.getContentAsString()).contains("\"code\":\"" + code.name() + "\"")
                .contains("\"timestamp\":\"2026-02-19T10:00:00.123Z\"");
        assertAllHeaders();
    }

    @Test
    void sendErrorNeverEchoesTheMessage() throws IOException {
        wrapper.sendError(404, "secret path /etc/passwd Apache Tomcat/11");

        assertThat(container.getStatus()).isEqualTo(404);
        assertThat(container.getContentAsString()).doesNotContain("secret").doesNotContain("passwd")
                .doesNotContain("Tomcat").contains("\"code\":\"NOT_FOUND\"");
        assertAllHeaders();
    }

    @Test
    void securityHeadersAreSetAtCreationAndSurviveReset() {
        assertAllHeaders();

        container.reset();
        assertThat(container.getHeader("X-Frame-Options")).isNull();
        wrapper.reset();

        assertAllHeaders();
    }

    @Test
    void sendErrorAfterResetKeepsTheHeadersAndWritesTheEnvelope() throws IOException {
        wrapper.setHeader("X-Frame-Options", "SAMEORIGIN");
        wrapper.reset();
        wrapper.sendError(500, "boom");

        assertAllHeaders();
        assertThat(container.getContentAsString()).contains("\"code\":\"INTERNAL_ERROR\"").doesNotContain("boom");
    }

    @Test
    void sendErrorOnACommittedResponseChangesNothing() throws IOException {
        wrapper.getWriter().write("done");
        wrapper.flushBuffer();

        wrapper.sendError(404);

        assertThat(container.getStatus()).isEqualTo(200);
        assertThat(container.getContentAsString()).isEqualTo("done");
    }
}
