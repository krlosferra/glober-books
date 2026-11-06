package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.repository.BookRepository;
import com.globant.globerbooks.support.LibrarySeeder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real-port tests of get, delete and the status change (US-001.9, US-006, US-007, US-008; spec 5.3, 5.4, 5.7;
 * S-04, S-06, S-07, S-11, T-14, T-15, T-19, T-38, C-04, E-18, E-24 row 1, E-27 rows 1 and 2). The library is
 * seeded through the repository. Expected values are copied from the spec.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookStoryIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String UNKNOWN = "00000000-0000-4000-8000-000000000000";
    private static final String STAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
    private static final Instant ADDED = Instant.parse("2026-02-01T08:00:00.000Z");
    private static final Instant DONE = Instant.parse("2026-02-10T09:00:00.000Z");

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("story.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        repository.deleteAllInBatch();
    }

    /** One answer: status, body text and the elapsed milliseconds. */
    private record Reply(int status, String body, long millis) {
        String code(JsonMapper mapper) {
            return mapper.readTree(body).path("code").asString();
        }
    }

    private Reply call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        if (!"GET".equals(method)) {
            builder.header("Content-Type", "application/json");
        }
        long start = System.nanoTime();
        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Reply reply = new Reply(response.statusCode(), response.body(), (System.nanoTime() - start) / 1_000_000);
        String shown = path.length() > 70 ? path.substring(0, 70) + "..." : path;
        System.out.println("EVIDENCE | " + method + " " + shown + " | body="
                + (body == null ? "none" : "'" + body + "'") + " | " + reply.status() + " | "
                + (reply.status() == 200 || reply.status() == 201 ? "-" : reply.code(json)) + " | "
                + reply.millis() + " ms");
        return reply;
    }

    private void store(String id, BookStatus status, Integer rating, Instant completed) {
        repository.save(new Book(id, "Title " + id.substring(id.length() - 2), "Author", status, rating, ADDED,
                completed));
    }

    private JsonNode tree(Reply reply) {
        return json.readTree(reply.body());
    }

    private void expectError(Reply reply, int status, String code) {
        assertThat(reply.status()).isEqualTo(status);
        assertThat(tree(reply).path("code").asString()).isEqualTo(code);
    }

    @Test
    void getReturnsTheBookJustCreated() throws Exception {
        Reply created = call("POST", BOOKS, "{\"title\":\"Clean Code\",\"author\":\"Robert Martin\"}");
        String id = tree(created).path("id").asString();

        Reply read = call("GET", BOOKS + "/" + id, null);

        assertThat(created.status()).isEqualTo(201);
        assertThat(read.status()).isEqualTo(200);
        assertThat(read.body()).isEqualTo(created.body());
    }

    @Test
    void getOfAnUnknownWellFormedIdIsBookNotFound() throws Exception {
        expectError(call("GET", BOOKS + "/" + UNKNOWN, null), 404, "BOOK_NOT_FOUND");
    }

    @Test
    void malformedIdsAreMalformedRequestOnEveryIdEndpoint() throws Exception {
        String valid = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
        for (String bad : new String[] {valid.toUpperCase(java.util.Locale.ROOT), "not-a-uuid",
            "1%27%20OR%20%271%27%3D%271"}) {
            expectError(call("GET", BOOKS + "/" + bad, null), 400, "MALFORMED_REQUEST");
            expectError(call("DELETE", BOOKS + "/" + bad, null), 400, "MALFORMED_REQUEST");
            expectError(call("PUT", BOOKS + "/" + bad + "/status", "{\"status\":\"reading\"}"), 400,
                    "MALFORMED_REQUEST");
        }
    }

    @Test
    void anyPercentInTheIdSegmentIsMalformedRequest() throws Exception {
        expectError(call("GET", BOOKS + "/%33f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b", null), 400, "MALFORMED_REQUEST");
    }

    @Test
    void wrongMethodOnAnIdPathIsMethodNotAllowedBeforeTheIdFormat() throws Exception {
        expectError(call("POST", BOOKS + "/not-a-uuid", "{}"), 405, "METHOD_NOT_ALLOWED");
        expectError(call("GET", BOOKS + "/not-a-uuid/status", null), 405, "METHOD_NOT_ALLOWED");
        expectError(call("GET", BOOKS + "/not-a-uuid/other", null), 404, "NOT_FOUND");
    }

    @Test
    void everyValidStatusIsAppliedAndReturned() throws Exception {
        store(UNKNOWN.replace("0000-4", "0001-4"), BookStatus.UNREAD, null, null);
        String id = UNKNOWN.replace("0000-4", "0001-4");
        for (String status : new String[] {"reading", "completed", "unread"}) {
            Reply reply = call("PUT", BOOKS + "/" + id + "/status", "{\"status\":\"" + status + "\"}");
            assertThat(reply.status()).isEqualTo(200);
            assertThat(tree(reply).path("status").asString()).isEqualTo(status);
            assertThat(tree(reply).size()).isEqualTo(7);
        }
    }

    @Test
    void enteringCompletedSetsTheDateAndLeavingClearsRatingAndDate() throws Exception {
        String id = UNKNOWN.replace("0000-4", "0002-4");
        store(id, BookStatus.READING, null, null);

        JsonNode entered = tree(call("PUT", BOOKS + "/" + id + "/status", "{\"status\":\"completed\"}"));
        repository.findById(id).ifPresent(book -> {
            book.assignRating(4);
            repository.save(book);
        });
        JsonNode left = tree(call("PUT", BOOKS + "/" + id + "/status", "{\"status\":\"reading\"}"));

        assertThat(entered.path("date_completed").asString()).matches(STAMP);
        assertThat(entered.path("rating").isNull()).isTrue();
        assertThat(left.path("rating").isNull()).isTrue();
        assertThat(left.path("date_completed").isNull()).isTrue();
        assertThat(tree(call("GET", BOOKS + "/" + id, null)).path("rating").isNull()).isTrue();
    }

    @Test
    void completedToCompletedChangesNothing() throws Exception {
        String id = UNKNOWN.replace("0000-4", "0003-4");
        store(id, BookStatus.COMPLETED, 5, DONE);

        Reply reply = call("PUT", BOOKS + "/" + id + "/status", "{\"status\":\"completed\"}");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(tree(reply).path("date_completed").asString()).isEqualTo("2026-02-10T09:00:00.000Z");
        assertThat(tree(reply).path("rating").asInt()).isEqualTo(5);
    }

    @Test
    void sameStateChangesOnUnreadAndReadingChangeNothing() throws Exception {
        String unread = UNKNOWN.replace("0000-4", "0004-4");
        String reading = UNKNOWN.replace("0000-4", "0005-4");
        store(unread, BookStatus.UNREAD, null, null);
        store(reading, BookStatus.READING, null, null);
        String before = call("GET", BOOKS, null).body();

        Reply first = call("PUT", BOOKS + "/" + unread + "/status", "{\"status\":\"unread\"}");
        Reply second = call("PUT", BOOKS + "/" + reading + "/status", "{\"status\":\"reading\"}");

        assertThat(first.status()).isEqualTo(200);
        assertThat(second.status()).isEqualTo(200);
        assertThat(call("GET", BOOKS, null).body()).isEqualTo(before);
    }

    @Test
    void invalidStatusValuesAreInvalidStatusAndWrongTypesAreMalformed() throws Exception {
        String id = UNKNOWN.replace("0000-4", "0006-4");
        store(id, BookStatus.UNREAD, null, null);
        for (String body : new String[] {"{\"status\":\"Completed\"}", "{\"status\":\"\"}", "{\"status\":null}",
            "{}"}) {
            Reply reply = call("PUT", BOOKS + "/" + id + "/status", body);
            expectError(reply, 400, "INVALID_STATUS");
            assertThat(tree(reply).path("details").get(0).path("field").asString()).isEqualTo("status");
        }
        for (String body : new String[] {"{\"status\":5}", "{\"status\":true}", "{\"status\":\"reading\",\"x\":1}",
            "[]", "{\"status\":"}) {
            expectError(call("PUT", BOOKS + "/" + id + "/status", body), 400, "MALFORMED_REQUEST");
        }
    }

    @Test
    void fieldValuesAreJudgedBeforeExistence() throws Exception {
        expectError(call("PUT", BOOKS + "/" + UNKNOWN + "/status", "{\"status\":\"Done\"}"), 400, "INVALID_STATUS");
        expectError(call("PUT", BOOKS + "/" + UNKNOWN + "/status", "{\"status\":5}"), 400, "MALFORMED_REQUEST");
        expectError(call("PUT", BOOKS + "/" + UNKNOWN + "/status", "{\"status\":\"reading\"}"), 404,
                "BOOK_NOT_FOUND");
    }

    @Test
    void deleteReturnsTheRemovedBookAndTheSecondDeleteIsNotFound() throws Exception {
        String id = tree(call("POST", BOOKS, "{\"title\":\"T\",\"author\":\"A\"}")).path("id").asString();
        String stored = call("GET", BOOKS + "/" + id, null).body();

        Reply first = call("DELETE", BOOKS + "/" + id, null);
        Reply second = call("DELETE", BOOKS + "/" + id, null);

        assertThat(first.status()).isEqualTo(200);
        assertThat(first.body()).isEqualTo(stored);
        expectError(second, 404, "BOOK_NOT_FOUND");
        expectError(call("GET", BOOKS + "/" + id, null), 404, "BOOK_NOT_FOUND");
        assertThat(tree(call("GET", BOOKS, null)).path("total").asInt()).isZero();
    }

    @Test
    void deleteWithoutBodyPassesTheGateAndAnyBodyIsMalformedRequest() throws Exception {
        String id = UNKNOWN.replace("0000-4", "0007-4");
        store(id, BookStatus.UNREAD, null, null);

        expectError(call("DELETE", BOOKS + "/" + id, " "), 400, "MALFORMED_REQUEST");
        expectError(call("DELETE", BOOKS + "/" + id, "{}"), 400, "MALFORMED_REQUEST");
        expectError(call("GET", BOOKS, "{}"), 400, "MALFORMED_REQUEST");
        assertThat(call("DELETE", BOOKS + "/" + id, null).status()).isEqualTo(200);
    }

    @Test
    void failedRequestsLeaveTheLibraryIdentical() throws Exception {
        String id = UNKNOWN.replace("0000-4", "0008-4");
        store(id, BookStatus.COMPLETED, 3, DONE);
        store(UNKNOWN.replace("0000-4", "0009-4"), BookStatus.READING, null, null);
        String before = call("GET", BOOKS, null).body();

        call("GET", BOOKS + "/NOT-A-UUID", null);
        call("GET", BOOKS + "/" + UNKNOWN, null);
        call("DELETE", BOOKS + "/" + UNKNOWN, null);
        call("DELETE", BOOKS + "/" + id, "{}");
        call("PUT", BOOKS + "/" + UNKNOWN + "/status", "{\"status\":\"reading\"}");
        call("PUT", BOOKS + "/not-a-uuid/status", "{\"status\":\"reading\"}");
        for (String body : new String[] {"{\"status\":\"Done\"}", "{}", "{\"status\":null}", "{\"status\":5}",
            "{\"status\":true}", "{\"status\":\"unread\",\"rating\":1}", "{\"status\":"}) {
            call("PUT", BOOKS + "/" + id + "/status", body);
        }

        assertThat(call("GET", BOOKS, null).body()).isEqualTo(before);
    }

    @Test
    void atCapacityListStatusChangeAndDeleteWorkThenOneAddFitsAndTheNextIsLibraryFull() throws Exception {
        LibrarySeeder.seedShort(repository, 5000);

        Reply list = call("GET", BOOKS, null);
        Reply change = call("PUT", BOOKS + "/" + LibrarySeeder.idOf(10) + "/status", "{\"status\":\"completed\"}");
        Reply delete = call("DELETE", BOOKS + "/" + LibrarySeeder.idOf(11), null);
        Reply fits = call("POST", BOOKS, "{\"title\":\"One more\",\"author\":\"Someone\"}");
        Reply full = call("POST", BOOKS, "{\"title\":\"Yet another\",\"author\":\"Someone\"}");

        assertThat(tree(list).path("total").asInt()).isEqualTo(5000);
        assertThat(change.status()).isEqualTo(200);
        assertThat(delete.status()).isEqualTo(200);
        assertThat(fits.status()).isEqualTo(201);
        expectError(full, 409, "LIBRARY_FULL");
        assertThat(repository.count()).isEqualTo(5000);
    }
}
