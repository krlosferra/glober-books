package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port, full-context tests of the validation wiring (spec US-001.4 to .6, .10, BR-012 step 6,
 * T-09, T-13, T-34, T-35). Expected codes and messages are copied from the spec; every request and
 * response is printed as evidence.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookValidationIntegrationTest {

    private static final String INJECTION = "${7*7}";

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("validation.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        jdbc.update("DELETE FROM books");
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/books"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String shown = body.length() > 70 ? body.substring(0, 70) + "...(" + body.length() + " chars)" : body;
        String answer = response.body().length() > 300 ? response.body().substring(0, 300) + "..." : response.body();
        System.out.println("EVIDENCE POST " + shown + " -> " + response.statusCode() + " " + answer);
        return response;
    }

    private int books() {
        return jdbc.queryForObject("SELECT COUNT(id) FROM books", Integer.class);
    }

    @Test
    void emptyObjectGivesTitleAndAuthorEntries() throws Exception {
        HttpResponse<String> response = post("{}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"")
                .contains("\"details\":[{\"field\":\"title\",\"message\":\"is required\"},"
                        + "{\"field\":\"author\",\"message\":\"is required\"}]");
        assertThat(books()).isZero();
    }

    @Test
    void titleOf501CodePointsIsRejectedAndOf500Accepted() throws Exception {
        HttpResponse<String> rejected = post("{\"title\":\"" + "a".repeat(501) + "\",\"author\":\"b\"}");
        HttpResponse<String> accepted = post("{\"title\":\"" + "a".repeat(500) + "\",\"author\":\"b\"}");

        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.body()).contains("\"code\":\"VALIDATION_ERROR\"").contains(
                "\"details\":[{\"field\":\"title\",\"message\":\"must be between 1 and 500 characters\"}]");
        assertThat(accepted.statusCode()).isEqualTo(201);
    }

    @Test
    void authorWithZeroWidthSpaceIsRejectedAsNotAllowedCharacters() throws Exception {
        HttpResponse<String> response = post("{\"title\":\"a\",\"author\":\"Au\\u200Bthor\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"").contains(
                "\"details\":[{\"field\":\"author\",\"message\":\"contains characters that are not allowed\"}]");
    }

    @Test
    void onlyInvalidStatusGivesInvalidStatus() throws Exception {
        HttpResponse<String> response = post("{\"title\":\"a\",\"author\":\"b\",\"status\":\"Done\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"INVALID_STATUS\"").contains(
                "\"details\":[{\"field\":\"status\",\"message\":\"must be one of: unread, reading, completed\"}]");
    }

    @Test
    void invalidStatusWithEmptyTitleGivesValidationErrorTitleThenStatus() throws Exception {
        HttpResponse<String> response = post("{\"title\":\"\",\"author\":\"a\",\"status\":\"Done\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"").contains(
                "\"details\":[{\"field\":\"title\",\"message\":\"must not be empty\"},"
                        + "{\"field\":\"status\",\"message\":\"must be one of: unread, reading, completed\"}]");
    }

    @Test
    void explicitNullStatusIsInvalidStatusAndAbsentStatusIsUnread() throws Exception {
        HttpResponse<String> explicit = post("{\"title\":\"a\",\"author\":\"b\",\"status\":null}");
        HttpResponse<String> absent = post("{\"title\":\"a\",\"author\":\"b\"}");

        assertThat(explicit.statusCode()).isEqualTo(400);
        assertThat(explicit.body()).contains("\"code\":\"INVALID_STATUS\"").contains("\"field\":\"status\"");
        assertThat(absent.statusCode()).isEqualTo(201);
        assertThat(absent.body()).contains("\"status\":\"unread\"");
    }

    @Test
    void hostileTitleIsRejectedAndNothingEchoesTheExpressionOrItsResult(CapturedOutput output) throws Exception {
        String hostile = INJECTION + "a".repeat(495);
        String request = "{\"title\":\"" + hostile + "\",\"author\":\"b\"}";

        HttpResponse<String> response = post(request);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"")
                .contains("\"message\":\"must be between 1 and 500 characters\"")
                .doesNotContain(INJECTION).doesNotContain("49");
        assertThat(output.getAll().replace("EVIDENCE POST", "")).doesNotContain(hostile.substring(0, 20));
        assertThat(books()).isZero();
    }

    @Test
    void storesTheTrimmedNfcTextOfAnAcceptedBook() throws Exception {
        HttpResponse<String> response = post("{\"title\":\"  Cafe\\u0301 \",\"author\":\"b\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"title\":\"Café\"");
    }
}
