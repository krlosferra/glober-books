package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.FileFacts;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port contract tests of {@code POST} and {@code GET /api/v1/books} (spec 5.0, 5.1, 5.2) on a
 * temporary SQLite file. Expected values are copied from the spec; bodies are printed as evidence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookApiIntegrationTest {

    private static final String ID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String STAMP_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
    private static final String INSERT =
            "INSERT INTO books (id, title, author, status, rating, date_added, date_completed)"
            + " VALUES (?, ?, 'A', 'unread', NULL, ?, NULL)";

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("api.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        jdbc.update("DELETE FROM books");
    }

    private HttpResponse<String> send(String method, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/books"));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("EVIDENCE " + method + " " + (body == null ? "" : body.length() > 80
                ? body.substring(0, 80) + "..." : body) + " -> " + response.statusCode() + " " + response.body());
        return response;
    }

    @Test
    void addGivesCreatedWithTheSevenMembersInWireFormat() throws Exception {
        HttpResponse<String> response = send("POST", "{\"title\":\"Clean Code\",\"author\":\"Robert Martin\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/json;charset=UTF-8");
        Matcher matcher = Pattern.compile("\\{\"id\":\"(" + ID_PATTERN + ")\",\"title\":\"Clean Code\","
                + "\"author\":\"Robert Martin\",\"status\":\"unread\",\"rating\":null,"
                + "\"date_added\":\"(" + STAMP_PATTERN + ")\",\"date_completed\":null\\}")
                .matcher(response.body());
        assertThat(matcher.matches()).as(response.body()).isTrue();
        assertThat(matcher.group(2)).hasSize(24);
    }

    @Test
    void completedBookWritesBothDatesAs24CharacterUtcText() throws Exception {
        HttpResponse<String> response = send("POST",
                "{\"title\":\"T\",\"author\":\"A\",\"status\":\"completed\",\"allow_duplicate\":true}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).containsPattern("\"status\":\"completed\",\"rating\":null,"
                + "\"date_added\":\"" + STAMP_PATTERN + "\",\"date_completed\":\"" + STAMP_PATTERN + "\"\\}");
    }

    @Test
    void zeroMillisecondInstantsStillCarryThreeFractionDigits() throws Exception {
        jdbc.update(INSERT, "00000000-0000-4000-8000-00000000000a", "Zero", "2026-02-19T10:00:00.000Z");

        HttpResponse<String> response = send("GET", null);

        assertThat(response.body()).contains("\"date_added\":\"2026-02-19T10:00:00.000Z\"")
                .contains("\"date_completed\":null").contains("\"rating\":null");
    }

    @Test
    void emptyLibraryListsEmptyArrayAndZero() throws Exception {
        HttpResponse<String> response = send("GET", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"books\":[],\"total\":0}");
    }

    @Test
    void listIsNewestFirstWithTiesByIdAndNothingIsWritten() throws Exception {
        jdbc.update(INSERT, "00000000-0000-4000-8000-00000000000b", "Tie B", "2026-02-19T10:00:00.000Z");
        jdbc.update(INSERT, "00000000-0000-4000-8000-00000000000a", "Tie A", "2026-02-19T10:00:00.000Z");
        jdbc.update(INSERT, "00000000-0000-4000-8000-00000000000c", "Newest", "2026-02-20T10:00:00.000Z");
        jdbc.update(INSERT, "00000000-0000-4000-8000-00000000000d", "Oldest", "2026-02-18T10:00:00.000Z");
        Path database = dataDir.resolve("api.db");
        String hashBefore = FileFacts.sha256(database);

        HttpResponse<String> response = send("GET", null);

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> titles = Pattern.compile("\"title\":\"([^\"]+)\"").matcher(response.body()).results()
                .map(match -> match.group(1)).toList();
        assertThat(titles).containsExactly("Newest", "Tie A", "Tie B", "Oldest");
        assertThat(response.body()).endsWith("],\"total\":4}");
        assertThat(FileFacts.sha256(database)).isEqualTo(hashBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isEqualTo(4);
    }

    @Test
    void addedBooksAppearInTheList() throws Exception {
        send("POST", "{\"title\":\"One\",\"author\":\"A\"}");

        HttpResponse<String> response = send("GET", null);

        assertThat(response.body()).contains("\"title\":\"One\"").endsWith("\"total\":1}");
    }

    @Test
    void emptyTitleIsValidationErrorWithTheField() throws Exception {
        HttpResponse<String> response = send("POST", "{\"title\":\"\",\"author\":\"A\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"")
                .contains("\"details\":[{\"field\":\"title\",\"message\":\"must not be empty\"}]");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isZero();
    }

    @Test
    void titleOf500CodePointsIsAcceptedAnd501IsRejected() throws Exception {
        String emoji = "\uD83D\uDCD8";
        HttpResponse<String> accepted = send("POST", "{\"title\":\"" + emoji.repeat(500) + "\",\"author\":\"A\"}");
        HttpResponse<String> rejected = send("POST", "{\"title\":\"" + "a".repeat(501) + "\",\"author\":\"A\"}");

        assertThat(accepted.statusCode()).isEqualTo(201);
        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.body()).contains("\"code\":\"VALIDATION_ERROR\"").contains("\"field\":\"title\"");
    }

    @Test
    void authorOf201CodePointsIsRejected() throws Exception {
        HttpResponse<String> response = send("POST", "{\"title\":\"T\",\"author\":\"" + "b".repeat(201) + "\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"field\":\"author\"");
    }

    @Test
    void invalidStatusAloneIsInvalidStatus() throws Exception {
        HttpResponse<String> response = send("POST", "{\"title\":\"T\",\"author\":\"A\",\"status\":\"done\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"INVALID_STATUS\"")
                .contains("{\"field\":\"status\",\"message\":\"must be one of: unread, reading, completed\"}");
    }

    @Test
    void unknownMemberIsMalformedRequestWithoutDetails() throws Exception {
        HttpResponse<String> response = send("POST", "{\"title\":\"T\",\"author\":\"A\",\"isbn\":\"1\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"MALFORMED_REQUEST\"").doesNotContain("details")
                .doesNotContain("isbn");
    }
}
