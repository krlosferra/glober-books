package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port proof of the query grammar, the no-body rule and the early rejection of large declared
 * bodies (spec 5.0, BR-012 steps 3 to 5; Appendix D E-18, E-19, E-26, E-31, E-32, E-33, E-36, E-38,
 * T-20, T-39). Expected values are copied from the spec. Rows that name an id endpoint (E-19, E-24
 * row 1, E-27 rows 1 and 2, T-14) answer 404 until Task 31 adds those routes and are completed there.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TransportLimitsIntegrationTest.Fixture.class)
class TransportLimitsIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String JSON = "Content-Type: application/json";
    private static final String MALFORMED = "MALFORMED_REQUEST";

    @TempDir
    static Path dataDir;

    @TempDir
    static Path staticDir;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void writeFixture() throws IOException {
        Files.createDirectories(staticDir.resolve("css"));
        Files.writeString(staticDir.resolve("index.html"), "<!doctype html><title>t</title>");
        Files.writeString(staticDir.resolve("css/app.css"), "body{margin:0}");
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("limits.db"));
    }

    /** Replaces the production catalog with one built from the fixture directory. */
    @TestConfiguration
    static class Fixture {
        @Bean
        @Primary
        StaticAssetCatalog fixtureCatalog() {
            return new StaticAssetCatalog(staticDir.toUri().toString());
        }
    }

    private Answer send(String label, String method, String path, byte[] body, String... headers) throws IOException {
        Answer answer = new RawHttpClient(port).request(method, path, body, headers);
        String compact = answer.raw().replace("\r\n\r\n", " || ").replace("\r\n", " | ")
                .replaceAll("\\| (Cross-Origin|Referrer|Cache-Control|X-Content|X-Frame|Content-Security"
                        + "|Permissions)[^|]*", "");
        System.out.println("EVIDENCE " + label + " " + method + " "
                + path.substring(0, Math.min(path.length(), 60)) + " " + String.join(" ", headers)
                + " => " + compact.trim().substring(0, Math.min(compact.trim().length(), 260)));
        return answer;
    }

    private void expect(Answer answer, int status, String code) {
        assertThat(answer.status()).isEqualTo(status);
        if (code != null) {
            assertThat(answer.body()).contains("\"code\":\"" + code + "\"");
        }
    }

    @Test
    void e26QueryGrammarOnTheList() throws IOException {
        expect(send("E-26", "GET", BOOKS + "?", null), 200, null);
        expect(send("E-26", "GET", BOOKS + "?&", null), 400, MALFORMED);
        expect(send("E-26", "GET", BOOKS + "?status=reading&", null), 400, MALFORMED);
        expect(send("E-26", "GET", BOOKS + "?status=reading;x=1", null), 400, "INVALID_STATUS");
        // Tomcat rejects a raw [ or ] in the query before the application (finding F-1 of the delivery):
        // the spec row says MALFORMED_REQUEST, the container answers its own 400.
        assertThat(send("E-26 row 5 (container)", "GET", BOOKS + "?status[]=reading", null).status()).isEqualTo(400);
        expect(send("E-26 row 5 encoded", "GET", BOOKS + "?status%5B%5D=reading", null), 400, MALFORMED);
        expect(send("E-26", "GET", BOOKS + "?%73tatus=reading", null), 200, null);
        expect(send("E-26", "GET", BOOKS + "?status=%72eading", null), 200, null);
        expect(send("E-26", "GET", BOOKS + "?status=reading+", null), 400, "INVALID_STATUS");
    }

    @Test
    void t20StatusValueAndShapeRows() throws IOException {
        for (String query : new String[] {"?status=READING", "?status=", "?status"}) {
            Answer answer = send("T-20", "GET", BOOKS + query, null);
            expect(answer, 400, "INVALID_STATUS");
            assertThat(answer.body()).contains("\"details\":[{\"field\":\"status\"");
        }
        for (String query : new String[] {"?status=a&status=b", "?foo=1", "?Status=reading"}) {
            expect(send("T-20", "GET", BOOKS + query, null), 400, MALFORMED);
        }
        for (String status : new String[] {"unread", "reading", "completed"}) {
            expect(send("T-20 valid", "GET", BOOKS + "?status=" + status, null), 200, null);
        }
    }

    @Test
    void malformedEncodingInvalidUtf8AndOtherEndpointsAreMalformed() throws IOException {
        for (String query : new String[] {"?status=%zz", "?status=%", "?status=%ff", "?status=%c3%28"}) {
            expect(send("E-26 extra", "GET", BOOKS + query, null), 400, MALFORMED);
        }
        expect(send("E-26 extra", "POST", BOOKS + "?x=1", "{}".getBytes(StandardCharsets.UTF_8), JSON,
                "Content-Length: 2"), 400, MALFORMED);
    }

    @Test
    void e36TwoThousandQueryPairsOnTheList() throws IOException {
        expect(send("E-36 row 3", "GET", BOOKS + "?" + "a=b&".repeat(2_000), null), 400, MALFORMED);
    }

    @Test
    void e38StaticContentTakesNoQueryAndNoBody() throws IOException {
        expect(send("E-38", "GET", "/?x=1", null), 400, MALFORMED);
        expect(send("E-38", "GET", "/index.html?", null), 200, null);
        expect(send("E-38", "GET", "/", " ".getBytes(StandardCharsets.UTF_8), "Content-Length: 1"), 400, MALFORMED);
        expect(send("E-38", "GET", "/css/app.css?x=1", null), 400, MALFORMED);
    }

    @Test
    void e18AndT39BodyOnGetIsMalformedAndEmptyBodiesPass() throws IOException {
        byte[] space = " ".getBytes(StandardCharsets.UTF_8);
        expect(send("E-18 / T-39", "GET", BOOKS, space, "Content-Length: 1"), 400, MALFORMED);
        expect(send("E-18", "GET", BOOKS, "{}".getBytes(StandardCharsets.UTF_8), "Content-Length: 2"), 400, MALFORMED);
        expect(send("no body rule: zero length", "GET", BOOKS, null, "Content-Length: 0"), 200, null);
        expect(send("no body rule: no length", "GET", BOOKS, null), 200, null);
        expect(send("no body rule: empty chunked", "GET", BOOKS, "0\r\n\r\n".getBytes(StandardCharsets.UTF_8),
                "Transfer-Encoding: chunked"), 200, null);
        expect(send("no body rule: chunked space", "GET", BOOKS,
                "1\r\n \r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8), "Transfer-Encoding: chunked"), 400, MALFORMED);
    }

    @Test
    void e32ChunkedBodyOverTheCapIs413OnAnyRoute() throws IOException {
        byte[] chunked = RawHttpClient.chunkedSpaces(16_385);
        expect(send("E-32", "GET", "/api/v1/unknown", chunked, "Transfer-Encoding: chunked"), 413, "PAYLOAD_TOO_LARGE");
        expect(send("E-32", "GET", BOOKS, chunked, "Transfer-Encoding: chunked"), 413, "PAYLOAD_TOO_LARGE");
        expect(send("E-32", "DELETE", BOOKS + "/3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b", chunked, JSON,
                "Transfer-Encoding: chunked"), 413, "PAYLOAD_TOO_LARGE");
    }

    @Test
    void e31ExpectContinueWithOversizedLengthGets413AndNoContinue() throws IOException {
        Answer answer = send("E-31 Expect", "POST", BOOKS, null, JSON, "Content-Length: 16385",
                "Expect: 100-continue");

        expect(answer, 413, "PAYLOAD_TOO_LARGE");
        assertThat(answer.raw()).doesNotContain("100 Continue");
    }

    @Test
    void e33EarlyRejectedLargeDeclaredBodyAnswersWithinTwoSecondsWhileOthersAreServed() throws Exception {
        byte[] megabyte = new byte[1_048_576];
        long start = System.nanoTime();
        CompletableFuture<Answer> early = CompletableFuture.supplyAsync(() -> {
            try {
                return new RawHttpClient(port).request("POST", BOOKS, megabyte, "Content-Type: text/plain",
                        "Content-Length: 10485760");
            } catch (IOException failure) {
                throw new IllegalStateException(failure);
            }
        });
        Answer other = send("E-33 other request", "GET", BOOKS, null);
        Answer rejected = early.get();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        System.out.println("EVIDENCE E-33 status " + rejected.status() + " elapsed ms " + elapsed.toMillis());

        expect(other, 200, null);
        expect(rejected, 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }
}
