package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Real-port proof of routing, methods and Accept (spec 5.0; Appendix D E-01 to E-06, E-17, E-24,
 * E-27, E-29). Expected values are copied from the spec. Rows that name an id endpoint answer 404
 * until Task 31 adds those endpoints; the spec status for them is noted per test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RoutingIntegrationTest.Fixture.class)
class RoutingIntegrationTest {

    private static final String ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final String JSON = "Content-Type: application/json";
    private static final String BOOKS = "/api/v1/books";

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
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("routing.db"));
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

    private Answer send(String label, String method, String path, String... headers) throws IOException {
        Answer answer = new RawHttpClient(port).request(method, path, null, headers);
        String compact = answer.raw().replace("\r\n\r\n", " || ").replace("\r\n", " | ")
                .replaceAll("\\| (Cross-Origin|Referrer|Cache-Control|X-Content|X-Frame|Content-Security"
                        + "|Permissions)[^|]*", "");
        System.out.println("EVIDENCE " + label + " " + method + " " + path + " " + String.join(" ", headers)
                + " => " + compact.trim());
        return answer;
    }

    private void expect(Answer answer, int status, String code, String allow) {
        assertThat(answer.status()).isEqualTo(status);
        if (code != null) {
            assertThat(answer.body()).contains("\"code\":\"" + code + "\"");
            assertThat(answer.header("Content-Type")).isEqualToIgnoringCase("application/json;charset=UTF-8");
        }
        assertThat(answer.header("Allow")).isEqualTo(allow);
    }

    @Test
    void e01UnknownTrailingSlashAndSemicolonPathsAreNotFound() throws IOException {
        for (String path : new String[] {"/api/v1/unknown", BOOKS + "/", BOOKS + "/" + ID + "/",
            BOOKS + "/" + ID + ";x=1", BOOKS + ";x=1", BOOKS + ".json", "/index.html/"}) {
            expect(send("E-01", "GET", path), 404, "NOT_FOUND", null);
        }
    }

    @Test
    void e02WrongMethodOnTheCollectionIsMethodNotAllowed() throws IOException {
        for (String method : new String[] {"PUT", "DELETE", "PATCH"}) {
            Answer answer = send("E-02", method, BOOKS, JSON);
            expect(answer, 405, "METHOD_NOT_ALLOWED", "GET, POST");
        }
    }

    @Test
    void e02IdPathsAreNotRoutesYetAndAnswerNotFoundUntilTask31() throws IOException {
        expect(send("E-02 (spec 405, Task 31)", "GET", BOOKS + "/" + ID + "/status"), 404, "NOT_FOUND", null);
        expect(send("E-02 (spec 405, Task 31)", "POST", BOOKS + "/" + ID, JSON), 404, "NOT_FOUND", null);
    }

    @Test
    void e03HeadOnTheCollectionIsMethodNotAllowedWithAllow() throws IOException {
        Answer answer = send("E-03", "HEAD", BOOKS);

        assertThat(answer.status()).isEqualTo(405);
        assertThat(answer.header("Allow")).isEqualTo("GET, POST");
        assertThat(answer.body()).isEmpty();
    }

    @Test
    void e05OptionsNeedsAContentTypeFirstThenIsMethodNotAllowed() throws IOException {
        expect(send("E-05 row 1", "OPTIONS", BOOKS), 415, "UNSUPPORTED_MEDIA_TYPE", null);
        expect(send("E-05 row 2", "OPTIONS", BOOKS, JSON), 405, "METHOD_NOT_ALLOWED", "GET, POST");
    }

    @Test
    void e06AcceptGrammarOnTheList() throws IOException {
        expect(send("E-06 row 1", "GET", BOOKS, "Accept: text/xml"), 406, "NOT_ACCEPTABLE", null);
        expect(send("E-06 row 2", "GET", BOOKS, "Accept: application/json"), 200, null, null);
        expect(send("E-06 row 3", "GET", BOOKS), 200, null, null);
        expect(send("E-06 row 4", "GET", BOOKS, "Accept: */*"), 200, null, null);
        expect(send("E-06 extra", "GET", BOOKS, "Accept:"), 200, null, null);
        expect(send("E-06 extra", "GET", BOOKS, "Accept: application/*"), 200, null, null);
        expect(send("E-06 extra", "GET", BOOKS, "Accept: application/json;q=0"), 406, "NOT_ACCEPTABLE", null);
        expect(send("E-06 extra", "GET", BOOKS, "Accept: json"), 406, "NOT_ACCEPTABLE", null);
    }

    @Test
    void methodIsDecidedBeforeAccept() throws IOException {
        expect(send("order", "PUT", BOOKS, JSON, "Accept: text/xml"), 405, "METHOD_NOT_ALLOWED", "GET, POST");
    }

    @Test
    void staticContentIgnoresAccept() throws IOException {
        assertThat(send("static Accept", "GET", "/index.html", "Accept: text/xml").status()).isEqualTo(200);
    }

    @Test
    void e17PostOnThePageIsMethodNotAllowedWithGetOnly() throws IOException {
        expect(send("E-17", "POST", "/", JSON), 405, "METHOD_NOT_ALLOWED", "GET");
    }

    @Test
    void headAndOptionsOnPageAndStaticPathsAreMethodNotAllowed() throws IOException {
        for (String path : new String[] {"/", "/index.html", "/css/app.css"}) {
            Answer head = send("SEC-F-127", "HEAD", path);
            assertThat(head.status()).isEqualTo(405);
            assertThat(head.header("Allow")).isEqualTo("GET");
            expect(send("SEC-F-127", "OPTIONS", path, JSON), 405, "METHOD_NOT_ALLOWED", "GET");
        }
    }

    @Test
    void e24EncodedFixedSegmentAndEncodedDotSegmentAreNotFound() throws IOException {
        expect(send("E-24 row 2", "GET", "/api/v1/%62ooks"), 404, "NOT_FOUND", null);
        expect(send("E-24 row 3", "GET", BOOKS + "/%2e%2e"), 404, "NOT_FOUND", null);
        expect(send("E-24 extra", "GET", BOOKS + "/%2E%2e"), 404, "NOT_FOUND", null);
    }

    @Test
    void e27RouteComesBeforeIdFormat() throws IOException {
        expect(send("E-27 row 3", "GET", BOOKS + "/not-a-uuid/other"), 404, "NOT_FOUND", null);
    }

    @Test
    void e29EveryMethodNotAllowedCarriesExactlyTheDefinedMethods() throws IOException {
        for (String method : new String[] {"PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"}) {
            Answer answer = send("E-29", method, BOOKS, JSON);
            assertThat(answer.header("Allow")).isEqualTo("GET, POST").doesNotContain("HEAD").doesNotContain("OPTIONS");
        }
        assertThat(send("E-29", "POST", "/index.html", JSON).header("Allow")).isEqualTo("GET");
    }
}
