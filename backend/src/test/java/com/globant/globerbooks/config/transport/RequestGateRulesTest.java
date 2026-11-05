package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.globant.globerbooks.error.ErrorBodyFactory;
import com.globant.globerbooks.error.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Gate stage 9 (id as received, query grammar, no-body rule) with a test-only route table that
 * already knows the id routes Task 31 will add (the real table has none yet). Expected values come
 * from the spec: T-14, T-20, T-39, E-18, E-19, E-26, E-27, E-38.
 */
class RequestGateRulesTest {

    private static final int PORT = 18080;
    private static final String BOOKS = "/api/v1/books";
    private static final String ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";

    private final RouteTable routes = mock(RouteTable.class);
    private final RequestGateFilter filter = new RequestGateFilter(new ErrorResponseWriter(
            new ErrorBodyFactory(Clock.fixed(Instant.parse("2026-02-19T10:00:00.123Z"), ZoneOffset.UTC))), routes);
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicBoolean reached = new AtomicBoolean();
    private final MockHttpServletRequest[] seen = new MockHttpServletRequest[1];

    private MockHttpServletRequest request(String method, String path, String query) {
        when(routes.decide(any(), any())).thenReturn(new RouteTable.Decision(RouteTable.Outcome.FOUND, null));
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setQueryString(query);
        request.setLocalPort(PORT);
        request.addHeader("Host", "localhost:" + PORT);
        if (!"GET".equals(method)) {
            request.setContentType("application/json");
        }
        seen[0] = request;
        return request;
    }

    private void run(MockHttpServletRequest request) throws Exception {
        FilterChain chain = (req, res) -> reached.set(true);
        filter.doFilter(request, response, chain);
    }

    private void assertRejected(int status, String code) throws Exception {
        assertThat(reached).isFalse();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentAsString()).contains("\"code\":\"" + code + "\"");
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/v1/books/", "DELETE,/api/v1/books/", "PUT,/api/v1/books/@/status",
        "PUT,/api/v1/books/@/rating", "DELETE,/api/v1/books/@/rating"})
    void badIdGives400Malformed(String method, String template) throws Exception {
        for (String bad : new String[] {ID.toUpperCase(), "not-a-uuid", "1%27%20OR%20%271%27%3D%271",
            "%33f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b"}) {
            MockHttpServletResponse fresh = new MockHttpServletResponse();
            String path = template.contains("@") ? template.replace("@", bad) : template + bad;
            filter.doFilter(request(method, path, null), fresh, (req, res) -> reached.set(true));
            assertThat(fresh.getStatus()).as(path).isEqualTo(400);
            assertThat(fresh.getContentAsString()).contains("\"code\":\"MALFORMED_REQUEST\"");
        }
        assertThat(reached).isFalse();
    }

    @Test
    void wellFormedIdPassesTheGate() throws Exception {
        run(request("GET", BOOKS + "/" + ID, null));

        assertThat(reached).isTrue();
    }

    @Test
    void routeAndMethodAreJudgedBeforeTheId() throws Exception {
        when(routes.decide(any(), eq("POST"))).thenReturn(new RouteTable.Decision(RouteTable.Outcome.METHOD_NOT_ALLOWED,
                "GET, DELETE"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", BOOKS + "/not-a-uuid");
        request.setLocalPort(PORT);
        request.addHeader("Host", "localhost:" + PORT);
        request.setContentType("application/json");

        run(request);

        assertRejected(405, "METHOD_NOT_ALLOWED");
    }

    @Test
    void nonListEndpointRejectsAnyQuery() throws Exception {
        run(request("GET", BOOKS + "/" + ID, "x=1"));

        assertRejected(400, "MALFORMED_REQUEST");
    }

    @Test
    void emptyQueryPassesOnAStaticPath() throws Exception {
        run(request("GET", "/index.html", ""));

        assertThat(reached).isTrue();
    }

    @Test
    void staticPathRejectsAQuery() throws Exception {
        run(request("GET", "/", "x=1"));

        assertRejected(400, "MALFORMED_REQUEST");
    }

    @Test
    void postOnTheCollectionRejectsAQuery() throws Exception {
        MockHttpServletRequest request = request("POST", BOOKS, "status=reading");
        request.setContentType("application/json");

        run(request);

        assertRejected(400, "MALFORMED_REQUEST");
    }

    @Test
    void listPassesTheDecodedStatusAsAnAttribute() throws Exception {
        run(request("GET", BOOKS, "status=%72eading"));

        assertThat(reached).isTrue();
        assertThat(seen[0].getAttribute(QueryPolicy.STATUS_ATTRIBUTE)).isEqualTo("reading");
    }

    @ParameterizedTest
    @CsvSource({"status=READING", "status=", "status", "status=reading+"})
    void wrongStatusValueGives400InvalidStatusWithDetails(String query) throws Exception {
        run(request("GET", BOOKS, query));

        assertRejected(400, "INVALID_STATUS");
        assertThat(response.getContentAsString()).contains("\"details\":[{\"field\":\"status\"");
    }

    @ParameterizedTest
    @CsvSource({"status=a&status=b", "foo=1", "Status=reading", "&", "status=%zz"})
    void badShapeGives400Malformed(String query) throws Exception {
        run(request("GET", BOOKS, query));

        assertRejected(400, "MALFORMED_REQUEST");
    }

    @ParameterizedTest
    @CsvSource({"GET," + BOOKS, "DELETE," + BOOKS + "/" + ID, "GET,/"})
    void bodyOnGetOrDeleteGives400Malformed(String method, String path) throws Exception {
        MockHttpServletRequest request = request(method, path, null);
        request.setContent(" ".getBytes());

        run(request);

        assertRejected(400, "MALFORMED_REQUEST");
    }

    @Test
    void zeroLengthChunkedBodyOnGetPasses() throws Exception {
        MockHttpServletRequest request = request("GET", BOOKS, null);
        request.addHeader("Transfer-Encoding", "chunked");

        run(request);

        assertThat(reached).isTrue();
    }

    @Test
    void noContentLengthOnDeletePasses() throws Exception {
        run(request("DELETE", BOOKS + "/" + ID, null));

        assertThat(reached).isTrue();
    }
}
