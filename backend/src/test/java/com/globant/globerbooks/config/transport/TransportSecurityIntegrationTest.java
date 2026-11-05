package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ErrorProbeConfig;
import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port transport security baseline of Task 08 on the product endpoint: header envelope per response
 * class (E-21, E-23), Host, Origin, Fetch-Metadata, Content-Type and body-cap rows (E-04, E-07 to E-14),
 * container-level answers (SEC-F-066 c) and the round trip with checkpoint logs. Prints the evidence table.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@Import(ErrorProbeConfig.class)
class TransportSecurityIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String JSON = "Content-Type: application/json";
    private static final String VALID = "{\"title\":\"Dune\",\"author\":\"Herbert\"}";
    private static final List<String> ENVELOPE = List.of("Content-Security-Policy", "X-Content-Type-Options",
            "X-Frame-Options", "Referrer-Policy", "Cross-Origin-Opener-Policy", "Cross-Origin-Resource-Policy",
            "Permissions-Policy", "Cache-Control");

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private RawHttpClient raw;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("transport.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        raw = new RawHttpClient(port);
        jdbc.update("DELETE FROM books");
    }

    private Answer show(String label, Answer answer) {
        String type = String.valueOf(answer.header("Content-Type"));
        String body = answer.body().replace("\r", "").replace("\n", " ");
        System.out.println("EVIDENCE " + label + " => " + answer.status() + " | Content-Type: " + type
                + " | Server: " + answer.header("Server") + " | headers=" + answer.headers().size() + " | "
                + (body.length() > 120 ? body.substring(0, 120) + "..." : body));
        return answer;
    }

    private Answer expect(String label, Answer answer, int status, String code) {
        show(label, answer);
        assertThat(answer.status()).as(label).isEqualTo(status);
        if (code != null) {
            assertThat(answer.body()).as(label).contains("\"code\":\"" + code + "\"");
        }
        return answer;
    }

    private void assertEnvelope(Answer answer) {
        for (String name : ENVELOPE) {
            assertThat(answer.header(name)).as(name).isNotBlank();
        }
        assertThat(answer.header("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(answer.header("X-Frame-Options")).isEqualTo("DENY");
        assertThat(answer.header("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(answer.header("Cross-Origin-Opener-Policy")).isEqualTo("same-origin");
        assertThat(answer.header("Cross-Origin-Resource-Policy")).isEqualTo("same-origin");
        assertThat(answer.header("Cache-Control")).isEqualTo("no-store");
        assertThat(answer.header("Content-Security-Policy")).startsWith("default-src 'self';");
        assertThat(answer.header("Permissions-Policy")).contains("camera=()", "microphone=()", "geolocation=()");
        assertThat(answer.header("Server")).isNull();
        assertThat(answer.header("Set-Cookie")).isNull();
        assertThat(answer.header("Location")).isNull();
        assertThat(answer.headers().stream().map(line -> line.toLowerCase(Locale.ROOT)))
                .noneMatch(line -> line.startsWith("access-control-"));
    }

    private Answer post(String body, String... headers) throws IOException {
        String[] all = new String[headers.length + 1];
        System.arraycopy(headers, 0, all, 0, headers.length);
        all[headers.length] = "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length;
        return raw.request("POST", BOOKS, body.getBytes(StandardCharsets.UTF_8), all);
    }

    private static String padded(int total) {
        return VALID + " ".repeat(total - VALID.length());
    }

    @Test
    void everyResponseClassCarriesTheEnvelopeAndNoForbiddenHeader() throws IOException {
        jdbc.execute("CREATE TRIGGER boom BEFORE INSERT ON books BEGIN SELECT RAISE(ABORT, 'forced'); END");
        Answer failing;
        try {
            failing = expect("500 forced by a database trigger", post(VALID, JSON), 500, "INTERNAL_ERROR");
        } finally {
            jdbc.execute("DROP TRIGGER boom");
        }
        List<Answer> answers = List.of(
                expect("200 GET", raw.request("GET", BOOKS, null), 200, null),
                expect("201 POST", post(VALID, JSON), 201, null),
                expect("400 Host", raw.request("GET", BOOKS, null, "Host: evil.example"), 400, "INVALID_HOST"),
                expect("400 malformed body", post("{", JSON), 400, "MALFORMED_REQUEST"),
                expect("403 Origin", raw.request("GET", BOOKS, null, "Origin: http://evil.example"), 403,
                        "FORBIDDEN"),
                expect("404 unknown path", raw.request("GET", "/nothing", null), 404, "NOT_FOUND"),
                expect("405 DELETE on books", raw.request("DELETE", BOOKS, null, JSON, "Content-Length: 0"), 405,
                        "METHOD_NOT_ALLOWED"),
                expect("413 chunked 16385", raw.send("POST " + BOOKS + " HTTP/1.1\r\nHost: localhost:" + port
                        + "\r\n" + JSON + "\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n",
                        RawHttpClient.chunkedSpaces(16_385)), 413, "PAYLOAD_TOO_LARGE"),
                expect("415 text/plain", post(VALID, "Content-Type: text/plain"), 415, "UNSUPPORTED_MEDIA_TYPE"),
                failing);
        answers.forEach(this::assertEnvelope);
        assertThat(failing.body()).doesNotContain("forced").doesNotContain("SQL").doesNotContain("Exception");
    }

    @Test
    void productEndpointRefusesForgedHostBadOriginCrossSiteAndWrongContentType() throws IOException {
        for (String host : new String[] {"evil.example", "localhost", "localhost:1", "[::1]:" + port}) {
            expect("GET books Host " + host, raw.request("GET", BOOKS, null, "Host: " + host), 400, "INVALID_HOST");
            expect("POST books Host " + host, post(VALID, JSON, "Host: " + host), 400, "INVALID_HOST");
        }
        for (String origin : new String[] {"http://evil.example", "null", "http://localhost:1"}) {
            expect("GET books Origin " + origin, raw.request("GET", BOOKS, null, "Origin: " + origin), 403,
                    "FORBIDDEN");
            expect("POST books Origin " + origin, post(VALID, JSON, "Origin: " + origin), 403, "FORBIDDEN");
        }
        for (String site : new String[] {"cross-site", "same-site"}) {
            expect("GET books Sec-Fetch-Site " + site, raw.request("GET", BOOKS, null, "Sec-Fetch-Site: " + site),
                    403, "FORBIDDEN");
            expect("POST books Sec-Fetch-Site " + site, post(VALID, JSON, "Sec-Fetch-Site: " + site), 403,
                    "FORBIDDEN");
        }
        for (String type : new String[] {"Content-Type: text/plain", "Content-Type: application/jsonx",
            "Content-Type: application/json; charset=ISO-8859-1"}) {
            expect("POST books " + type, post(VALID, type), 415, "UNSUPPORTED_MEDIA_TYPE");
        }
        expect("POST books no content type", post(VALID), 415, "UNSUPPORTED_MEDIA_TYPE");
        Answer listHost = show("E-10 one Host header 127.0.0.1:port, evil.example (container answer, R-23)",
                raw.request("GET", BOOKS, null, "Host: 127.0.0.1:" + port + ", evil.example"));
        assertThat(listHost.status()).isEqualTo(400);
        assertNoDisclosure(listHost);
        expect("POST books charset=utf-8", post(VALID, "Content-Type: application/json;charset=utf-8"), 201, null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isEqualTo(1);
    }

    @Test
    void optionsWithForeignOriginIsForbiddenAndAllowedHostsAndOriginsPass() throws IOException {
        Answer options = expect("E-04 OPTIONS evil origin", raw.request("OPTIONS", BOOKS, null,
                "Origin: http://evil.example", "Access-Control-Request-Method: POST"), 403, "FORBIDDEN");
        assertThat(options.headers().stream().map(line -> line.toLowerCase(Locale.ROOT)))
                .noneMatch(line -> line.startsWith("access-control-"));
        for (String host : new String[] {"LOCALHOST:" + port, "127.0.0.1:" + port, "localhost:" + port}) {
            expect("E-11 Host " + host, raw.request("GET", BOOKS, null, "Host: " + host), 200, null);
        }
        for (String accepted : new String[] {"Origin: http://localhost:" + port, "Origin: http://127.0.0.1:" + port,
            "Sec-Fetch-Site: same-origin", "Sec-Fetch-Site: none"}) {
            expect("E-13 " + accepted, raw.request("GET", BOOKS, null, accepted), 200, null);
        }
    }

    @Test
    void hostRowsWithoutOrWithTwoHostHeadersAreAnsweredAndTheServerKeepsServing() throws IOException {
        Answer none = show("E-10 HTTP/1.0 without Host", raw.send("GET " + BOOKS + " HTTP/1.0\r\n\r\n", null));
        assertThat(none.status()).isIn(400, 505);
        Answer two = show("E-10 two Host headers", raw.send("GET " + BOOKS + " HTTP/1.1\r\nHost: localhost:" + port
                + "\r\nHost: localhost:" + port + "\r\nConnection: close\r\n\r\n", null));
        assertThat(two.status()).isIn(400, 505);
        for (Answer answer : List.of(none, two)) {
            assertNoDisclosure(answer);
        }
        expect("next request", raw.request("GET", BOOKS, null), 200, null);
    }

    @Test
    void bodyCapBoundariesHoldOnContentLengthChunkedAndUnknownPaths() throws IOException {
        expect("E-09 16384 bytes", post(padded(16_384), JSON), 201, null);
        expect("E-09 16385 bytes", post(padded(16_385), JSON), 413, "PAYLOAD_TOO_LARGE");
        String chunkedHead = "POST " + BOOKS + " HTTP/1.1\r\nHost: localhost:" + port + "\r\n" + JSON
                + "\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n";
        expect("E-09 chunked 16385", raw.send(chunkedHead, RawHttpClient.chunkedSpaces(16_385)), 413,
                "PAYLOAD_TOO_LARGE");
        byte[] over = " ".repeat(16_385).getBytes(StandardCharsets.UTF_8);
        expect("E-09 16385 to unknown path", raw.request("POST", "/nothing", over, JSON, "Content-Length: 16385"),
                413, "PAYLOAD_TOO_LARGE");
        expect("next request", raw.request("GET", BOOKS, null), 200, null);
    }

    @Test
    void containerLevelRefusalsShowNoProductVersionOrReportAndTheNextRequestIsServed() throws IOException {
        String host = "Host: localhost:" + port + "\r\n";
        String[][] cases = {
            {"invalid request line", "GET / HTTP/1.1 junk junk\r\n" + host + "Connection: close\r\n\r\n"},
            {"method FOO with JSON content type", "FOO " + BOOKS + " HTTP/1.1\r\n" + host + JSON
                + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"},
            {"header over 8 KiB", "GET " + BOOKS + " HTTP/1.1\r\n" + host + "X-Big: " + "a".repeat(9000)
                + "\r\nConnection: close\r\n\r\n"},
            {"request line over 8 KiB", "GET /" + "a".repeat(9000) + " HTTP/1.1\r\n" + host
                + "Connection: close\r\n\r\n"}};
        for (String[] each : cases) {
            Answer answer = show(each[0], raw.send(each[1], null));
            assertThat(answer.status()).as(each[0]).isIn(400, 405, 414, 431, 501);
            assertNoDisclosure(answer);
            System.out.println("EVIDENCE " + each[0] + " body contains HTML tag: " + answer.body().contains("<"));
            expect("next request after " + each[0], raw.request("GET", BOOKS, null), 200, null);
        }
    }

    @Test
    void methodFooWithoutContentTypeIsRefusedByTheGateWith415() throws IOException {
        expect("method FOO without content type (gate answer, reported deviation)",
                raw.request("FOO", BOOKS, null), 415, "UNSUPPORTED_MEDIA_TYPE");
    }

    private void assertNoDisclosure(Answer answer) {
        assertThat(answer.raw()).doesNotContainIgnoringCase("tomcat").doesNotContainIgnoringCase("apache")
                .doesNotContainIgnoringCase("Server:").doesNotContainIgnoringCase("exception")
                .doesNotContainIgnoringCase("Type Exception Report").doesNotContainIgnoringCase("Description")
                .doesNotContainPattern("\\d+\\.\\d+\\.\\d+");
        assertThat(answer.header("Server")).isNull();
    }

    @Test
    void roundTripOnTheRealPortLogsNoBookData(CapturedOutput output) throws IOException {
        Answer created = expect("round trip POST", post("{\"title\":\"Zanzibar Secret Title\",\"author\":"
                + "\"Quentin Hidden Author\"}", JSON), 201, null);
        Answer listed = expect("round trip GET", raw.request("GET", BOOKS, null), 200, null);

        assertThat(created.body()).contains("\"title\":\"Zanzibar Secret Title\"");
        assertThat(listed.body()).contains("Zanzibar Secret Title").contains("Quentin Hidden Author");
        String logs = output.getAll().lines().filter(line -> !line.startsWith("EVIDENCE")).reduce("",
                (first, second) -> first + second + "\n");
        assertThat(logs).doesNotContain("Zanzibar").doesNotContain("Quentin").doesNotContain("Hidden Author");
        System.out.println("EVIDENCE checkpoint log lines: " + logs.lines().count() + ", book data in logs: false");
    }

    @Test
    void sendErrorInsideTheApplicationLeavesAsTheEnvelopeWithTheHeaders() throws IOException {
        Answer forced = expect("sendError(500, message) inside the application",
                raw.request("GET", BOOKS, null, "X-Probe-Send-Error: 500"), 500, "INTERNAL_ERROR");
        Answer unknown = expect("sendError(418) inside the application",
                raw.request("GET", BOOKS, null, "X-Probe-Send-Error: 418"), 500, "INTERNAL_ERROR");
        Answer reset = expect("reset then sendError(404) inside the application",
                raw.request("GET", BOOKS, null, "X-Probe-Send-Error: 404", "X-Probe-Reset: 1"), 404, "NOT_FOUND");
        for (Answer answer : List.of(forced, unknown, reset)) {
            assertEnvelope(answer);
            assertThat(answer.header("Content-Type")).isEqualToIgnoringCase("application/json;charset=UTF-8");
            assertThat(answer.raw()).doesNotContain("secret").doesNotContain("<html")
                    .doesNotContainIgnoringCase("tomcat");
        }
    }

    @Test
    void errorPathAnswersNotFoundInTheEnvelopeWithTheHeaders() throws IOException {
        for (String method : new String[] {"GET", "POST"}) {
            Answer answer = method.equals("GET") ? raw.request("GET", "/error", null)
                    : raw.request("POST", "/error", "{}".getBytes(StandardCharsets.UTF_8), JSON,
                            "Content-Length: 2");
            expect(method + " /error", answer, 404, "NOT_FOUND");
            assertEnvelope(answer);
            assertThat(answer.header("Content-Type")).isEqualToIgnoringCase("application/json;charset=UTF-8");
        }
    }
}
