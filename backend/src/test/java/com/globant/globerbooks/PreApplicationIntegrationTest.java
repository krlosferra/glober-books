package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FreshApp;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import com.globant.globerbooks.support.RawRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The container-level cases of Task 49 (criteria 1 to 4) on raw sockets against the packaged jar: answers given
 * by the server before the application (spec Appendix D E-10, E-15, E-20, E-24, E-24b, E-25, E-28, E-31, E-36,
 * E-37; DEC-033; SC-18, SC-29; R-13, R-23). The expected values come from the spec rows. A row that may be
 * answered before the application accepts status 400 with no product, no version and no echo; every row also
 * proves that the next request on a new connection is served. Each row prints one RAW evidence line.
 */
class PreApplicationIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String ECHO = "ZQXECHO";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String MALFORMED = "MALFORMED_REQUEST";

    @TempDir
    static Path temp;

    private static AppProcess app;
    private static RawRules rules;
    private static int port;

    @BeforeAll
    static void start() throws Exception {
        app = FreshApp.start(temp);
        port = app.api().port();
        rules = new RawRules(port);
    }

    @AfterAll
    static void stop() throws Exception {
        FreshApp.stopAndCheck(app);
    }

    private Answer raw(String label, String head, byte[] body) throws IOException {
        Answer answer = rules.client().send(head, body);
        assertThat(rules.record(label, answer)).as(label + ": the next request is served").isTrue();
        return answer;
    }

    private Answer line(String label, String requestLine, String... headers) throws IOException {
        return raw(label, RawRules.head(requestLine, port, headers), null);
    }

    private void preApplication(String label, Answer answer, int... statuses) {
        assertThat(RawRules.in(answer.status(), statuses)).as(label + " status " + answer.status()).isTrue();
        RawRules.assertNoDisclosure(answer, ECHO);
    }

    @Test
    void criterion1NoHostTwoHostsAndMalformedRequestLinesAreRefusedBeforeTheApplication() throws Exception {
        preApplication("E-37 no Host HTTP/1.1", raw("E-37 no Host HTTP/1.1",
                "GET " + BOOKS + " HTTP/1.1\r\nConnection: close\r\n\r\n", null), 400);
        preApplication("E-10 no Host HTTP/1.0", raw("E-10 no Host HTTP/1.0",
                "GET " + BOOKS + " HTTP/1.0\r\n\r\n", null), 400, 505);
        preApplication("E-10 two Host", line("E-10 two Host", "GET " + BOOKS + " HTTP/1.1",
                "Host: localhost:" + port, "Host: localhost:" + port), 400, 505);
        preApplication("E-37 version 9.9", line("E-37 version 9.9", "GET / HTTP/9.9"), 505);
        preApplication("E-37 space in target", line("E-37 space in target", "GET /a b HTTP/1.1"), 400);
    }

    @Test
    void criterion1ContentLengthFramingConflictsAreRefused() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.US_ASCII);
        String json = "Content-Type: application/json";
        preApplication("E-37 two Content-Length", raw("E-37 two Content-Length", RawRules.head("POST " + BOOKS
                + " HTTP/1.1", port, json, "Content-Length: 2", "Content-Length: 2"), body), 400);
        preApplication("E-37 Content-Length -1", raw("E-37 Content-Length -1", RawRules.head("POST " + BOOKS
                + " HTTP/1.1", port, json, "Content-Length: -1"), body), 400);
        preApplication("E-37 Content-Length and Transfer-Encoding", raw("E-37 CL and TE", RawRules.head("POST "
                + BOOKS + " HTTP/1.1", port, json, "Content-Length: 2", "Transfer-Encoding: chunked"), body), 400);
    }

    @Test
    void criterion1LinesAndHeadersAboveEightKibAreRefused() throws Exception {
        preApplication("E-20 line above 8 KiB", line("E-20 line 9000", "GET /" + ECHO + "a".repeat(9000)
                + " HTTP/1.1"), 400, 414, 431);
        preApplication("E-20 header above 8 KiB", line("E-20 header 9000", "GET " + BOOKS + " HTTP/1.1",
                "X-Big: " + ECHO + "a".repeat(9000)), 400, 414, 431);
        preApplication("E-36 line above 16 KiB", line("E-36 line 20000", "GET /" + ECHO + "a".repeat(20_000)
                + " HTTP/1.1"), 400, 414, 431);
    }

    @Test
    void criterion2BothTraceRequestsAreRefusedWith405BeforeTheApplication() throws Exception {
        Answer withType = line("E-28 TRACE with type", "TRACE " + BOOKS + " HTTP/1.1",
                "Content-Type: application/json", "X-Echo: " + ECHO);
        Answer withoutType = line("E-28 TRACE without type", "TRACE " + BOOKS + " HTTP/1.1", "X-Echo: " + ECHO);
        for (Answer answer : List.of(withType, withoutType)) {
            preApplication("E-28 TRACE", answer, 405);
            assertThat(RawRules.bodyClass(answer)).as("not an application envelope").isNotEqualTo("json-envelope");
        }
    }

    @Test
    void criterion2UnknownAndLowerCaseMethodsAreRefused() throws Exception {
        String json = "Content-Type: application/json";
        preApplication("E-28 FOO", line("E-28 FOO", "FOO " + BOOKS + " HTTP/1.1", json, "X-Echo: " + ECHO),
                400, 405, 501);
        preApplication("E-28 get", line("E-28 get", "get " + BOOKS + " HTTP/1.1", json, "X-Echo: " + ECHO),
                400, 405, 501);
    }

    @Test
    void criterion3ContainerRowsGiveTheEnvelopeOrAPreApplicationAnswerWithoutDisclosure() throws Exception {
        String id = UUID.randomUUID().toString();
        // {path, status of the application answer, code, whether a pre-application 400 is accepted}
        List<String[]> rows = List.of(
                new String[] {"/%2e%2e/%2e%2e/etc/passwd", "404", NOT_FOUND, "pre"},
                new String[] {"/..%2f..%2fetc/passwd", "404", NOT_FOUND, "pre"},
                new String[] {BOOKS + "/%33f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b", "400", MALFORMED, "app"},
                new String[] {"/api/v1/%62ooks", "404", NOT_FOUND, "app"},
                new String[] {BOOKS + "/%2e%2e", "404", NOT_FOUND, "app"},
                new String[] {BOOKS + "/.%2E", "404", NOT_FOUND, "app"},
                new String[] {BOOKS + "/" + id + "%2f", "404", NOT_FOUND, "pre"},
                new String[] {BOOKS + "/" + id + "%2F", "404", NOT_FOUND, "pre"},
                new String[] {BOOKS + "/" + id + "%5c", "404", NOT_FOUND, "pre"},
                new String[] {BOOKS + "/" + id + "%3b", "404", NOT_FOUND, "app"},
                new String[] {BOOKS + "/" + id + "\\x", "404", NOT_FOUND, "pre"},
                new String[] {BOOKS + "?status=%zz", "400", MALFORMED, "pre"},
                new String[] {BOOKS + "?status=%FF", "400", MALFORMED, "pre"},
                new String[] {BOOKS + "/%zz", "400", MALFORMED, "pre"},
                new String[] {BOOKS + "/%", "400", MALFORMED, "pre"},
                new String[] {"/%00", "404", NOT_FOUND, "pre"});
        for (String[] row : rows) {
            String label = "E-15/24/24b/25 " + row[0];
            Answer answer = line(label, "GET " + row[0] + " HTTP/1.1");
            boolean application = answer.status() == Integer.parseInt(row[1])
                    && answer.body().contains("\"code\":\"" + row[2] + "\"");
            if (!application) {
                assertThat(row[3]).as(label + " answered " + answer.status() + " " + RawRules.bodyClass(answer))
                        .isEqualTo("pre");
                preApplication(label, answer, 400);
            }
        }
    }

    @Test
    void criterion3TomcatFirstPagesAnswer400Or404WithoutDisclosure() throws Exception {
        for (String path : List.of("/%2e%2e/", "/..;/", "/%2f", "/%00", "/WEB-INF/web.xml", "/META-INF/x")) {
            Answer answer = line("SEC-F-151 " + path, "GET " + path + " HTTP/1.1");
            preApplication(path, answer, 400, 404);
        }
    }

    @Test
    void criterion4UpgradeRequestsAreAnsweredWith200AndNeverSwitchProtocols() throws Exception {
        Answer websocket = line("E-31 websocket", "GET / HTTP/1.1", "Upgrade: websocket", "Connection: Upgrade",
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==", "Sec-WebSocket-Version: 13");
        Answer h2c = line("E-31 h2c", "GET / HTTP/1.1", "Upgrade: h2c");
        Answer h2cSettings = line("E-31 h2c with settings", "GET / HTTP/1.1", "Upgrade: h2c",
                "Connection: Upgrade, HTTP2-Settings", "HTTP2-Settings: AAMAAABkAAQAAP__");
        for (Answer answer : List.of(websocket, h2c, h2cSettings)) {
            assertThat(answer.status()).isEqualTo(200);
            assertThat(answer.header("Upgrade")).isNull();
        }
    }
}
