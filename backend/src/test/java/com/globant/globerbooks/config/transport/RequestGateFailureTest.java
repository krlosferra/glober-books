package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.globant.globerbooks.error.ErrorBodyFactory;
import com.globant.globerbooks.error.ErrorResponseWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** SEC-F-113: an unexpected failure anywhere in the gate, before the controller, is the 500 envelope. */
class RequestGateFailureTest {

    private static final int PORT = 18080;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicBoolean reached = new AtomicBoolean();

    @Test
    void unexpectedFailureBeforeTheControllerBecomesTheEnvelopeWithTheHeaders() throws Exception {
        RouteTable failing = mock(RouteTable.class);
        when(failing.decide(any(), any())).thenThrow(new IllegalStateException("secret internal text"));
        RequestGateFilter broken = new RequestGateFilter(new ErrorResponseWriter(new ErrorBodyFactory(
                Clock.fixed(Instant.parse("2026-02-19T10:00:00.123Z"), ZoneOffset.UTC))), failing);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        request.setLocalPort(PORT);
        request.addHeader("Host", "localhost:" + PORT);

        broken.doFilter(request, response, (req, res) -> reached.set(true));

        assertThat(reached).isFalse();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("\"code\":\"INTERNAL_ERROR\"")
                .doesNotContain("secret");
        HeaderPolicy.SECURITY_HEADERS.forEach((name, value) -> assertThat(response.getHeader(name)).isEqualTo(value));
    }
}
