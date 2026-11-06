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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real-port tests of the rating endpoints (spec 5.5, 5.6; US-003; BR-002, BR-007, BR-012 steps 6 to 8, BR-013;
 * S-01, S-02, S-03, S-05, T-14, T-17, T-18, T-31, T-32, T-36, T-37, C-03, E-02, E-18, E-29, SEC-F-159, SEC-F-170).
 * Every request goes through the gate, the strict resolver, the advice and SQLite. Expected values are copied
 * from the spec.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookRatingIntegrationTest {

    private static final String BOOKS = "/api/v1/books/";
    private static final String UNKNOWN = "00000000-0000-4000-8000-000000000000";
    private static final String DONE_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final String READING_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6c";
    private static final String UNREAD_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6d";
    private static final Instant ADDED = Instant.parse("2026-02-01T08:00:00.000Z");
    private static final Instant DONE = Instant.parse("2026-02-10T09:00:00.000Z");
    private static final String MESSAGE = "must be an integer from 1 to 5";

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
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("rating.db"));
    }

    @BeforeEach
    void threeBooks() {
        repository.deleteAllInBatch();
        repository.save(new Book(DONE_ID, "Done", "Author", BookStatus.COMPLETED, null, ADDED, DONE));
        repository.save(new Book(READING_ID, "Reading", "Author", BookStatus.READING, null, ADDED, null));
        repository.save(new Book(UNREAD_ID, "Unread", "Author", BookStatus.UNREAD, null, ADDED, null));
    }

    /** One answer: status, content type, allow header, body and elapsed milliseconds. */
    private record Reply(int status, String type, String allow, String body, long millis) {
    }

    private Reply call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        long start = System.nanoTime();
        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Reply reply = new Reply(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""),
                response.headers().firstValue("Allow").orElse(null), response.body(),
                (System.nanoTime() - start) / 1_000_000);
        String shown = method + " " + path + " body=" + (body == null ? "none" : abbreviated(body));
        System.out.println("EVIDENCE | " + shown + " | " + reply.status() + " | "
                + (reply.status() == 200 ? "-" : tree(reply).path("code").asString()) + " | " + reply.millis() + " ms");
        return reply;
    }

    private static String abbreviated(String text) {
        return text.length() <= 48 ? text : text.substring(0, 24) + "..(" + text.length() + " chars)..";
    }

    private Reply rate(String id, String body) throws Exception {
        return call("PUT", BOOKS + id + "/rating", "{\"rating\":" + body + "}");
    }

    private JsonNode tree(Reply reply) {
        return json.readTree(reply.body());
    }

    private void expectError(Reply reply, int status, String code) {
        assertThat(reply.status()).as(reply.body()).isEqualTo(status);
        assertThat(tree(reply).path("code").asString()).isEqualTo(code);
    }

    private String stored(String id) throws Exception {
        return call("GET", BOOKS + id, null).body();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void ratesACompletedBookWithEachValueAndReturnsTheBook(int value) throws Exception {
        Reply reply = rate(DONE_ID, String.valueOf(value));

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.type()).isEqualToIgnoringCase("application/json;charset=UTF-8");
        assertThat(tree(reply).path("rating").asInt()).isEqualTo(value);
        assertThat(tree(reply).path("status").asString()).isEqualTo("completed");
        assertThat(tree(reply).path("date_completed").asString()).isEqualTo("2026-02-10T09:00:00.000Z");
        assertThat(stored(DONE_ID)).isEqualTo(reply.body());
    }

    @Test
    void replacesTheRatingAndClearingTwiceGivesTwoOkWithANullRating() throws Exception {
        rate(DONE_ID, "4");

        assertThat(tree(rate(DONE_ID, "2")).path("rating").asInt()).isEqualTo(2);
        for (int attempt = 0; attempt < 2; attempt++) {
            Reply cleared = call("DELETE", BOOKS + DONE_ID + "/rating", null);
            assertThat(cleared.status()).isEqualTo(200);
            assertThat(tree(cleared).path("rating").isNull()).isTrue();
            assertThat(tree(cleared).path("status").asString()).isEqualTo("completed");
        }
    }

    /** S-01: an unread book cannot be rated and stays unchanged; the same for a reading book. */
    @Test
    void ratingAnUnreadOrReadingBookIsRatingNotAllowedWithoutDetailsAndLeavesItUnchanged() throws Exception {
        for (String id : new String[] {UNREAD_ID, READING_ID}) {
            String before = stored(id);
            Reply reply = rate(id, "4");
            expectError(reply, 400, "RATING_NOT_ALLOWED");
            assertThat(tree(reply).has("details")).isFalse();
            assertThat(stored(id)).isEqualTo(before);
        }
    }

    /** T-36: the value is judged before the status. */
    @Test
    void anInvalidValueOnAnUnreadBookIsInvalidRatingAndThenAValidOneIsRatingNotAllowed() throws Exception {
        expectError(rate(UNREAD_ID, "7"), 400, "INVALID_RATING");
        expectError(rate(READING_ID, "null"), 400, "INVALID_RATING");
        expectError(rate(UNREAD_ID, "4"), 400, "RATING_NOT_ALLOWED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"7", "0", "6", "-1", "-0", "null", "5.0", "4.9", "1e0", "1E1", "99999999999999999999999",
        "1e999999999", "1e-999999999", "0.0000000000000000000000000000000000000001"})
    void invalidNumbersAndNullGiveInvalidRatingWithOneFixedDetailAndNoEcho(String value) throws Exception {
        String before = stored(DONE_ID);

        Reply reply = rate(DONE_ID, value);

        expectError(reply, 400, "INVALID_RATING");
        assertThat(tree(reply).path("details")).hasSize(1);
        assertThat(tree(reply).path("details").get(0).path("field").asString()).isEqualTo("rating");
        assertThat(tree(reply).path("details").get(0).path("message").asString()).isEqualTo(MESSAGE);
        assertThat(reply.body()).doesNotContain("999").doesNotContain("0.0000");
        assertThat(reply.millis()).isLessThan(300);
        assertThat(stored(DONE_ID)).isEqualTo(before);
    }

    /** T-37: 100 digits is a number judged by value (invalid), 101 is a parser limit (malformed, below). */
    @Test
    void missingRatingMemberAndHundredDigitIntegerAreInvalidRating() throws Exception {
        expectError(call("PUT", BOOKS + DONE_ID + "/rating", "{}"), 400, "INVALID_RATING");
        expectError(rate(DONE_ID, "9".repeat(100)), 400, "INVALID_RATING");
        expectError(rate(DONE_ID, "9".repeat(101)), 400, "MALFORMED_REQUEST");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"5\"", "true", "[5]", "{\"a\":1}", "01", "+5", "NaN", "Infinity", "5.", ".5", "0x5",
        "5e"})
    void wrongTypesAndInvalidLiteralsAreMalformedRequest(String value) throws Exception {
        String before = stored(DONE_ID);

        expectError(rate(DONE_ID, value), 400, "MALFORMED_REQUEST");

        assertThat(stored(DONE_ID)).isEqualTo(before);
    }

    /** SEC-F-159 through the real resolver: duplicate, unknown and extra members are refused. */
    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"completed\",\"rating\":5}", "{\"rating\":5,\"status\":\"completed\"}",
        "{\"rating\":4,\"rating\":5}", "{\"rating\":5,\"x\":1}", "[5]", "5", "{\"rating\":5} {}", "{\"rating\":"})
    void strictBodyRulesHoldOnTheRealPort(String body) throws Exception {
        String before = stored(DONE_ID);

        expectError(call("PUT", BOOKS + DONE_ID + "/rating", body), 400, "MALFORMED_REQUEST");

        assertThat(stored(DONE_ID)).isEqualTo(before);
    }

    /** T-31 nested rows; the first, cold request is a warm-up whose time is only reported. */
    @Test
    void deeplyNestedBodiesAreMalformedRequestWithinThreeHundredMilliseconds() throws Exception {
        String nested = "{\"rating\":" + "[".repeat(8000) + "]".repeat(8000) + "}";
        String unknown = "{\"rating\":5,\"y\":" + "{\"x\":".repeat(2000) + "1" + "}".repeat(2000) + "}";
        call("PUT", BOOKS + DONE_ID + "/rating", nested);

        Reply first = call("PUT", BOOKS + DONE_ID + "/rating", nested);
        Reply second = call("PUT", BOOKS + DONE_ID + "/rating", unknown);

        expectError(first, 400, "MALFORMED_REQUEST");
        expectError(second, 400, "MALFORMED_REQUEST");
        assertThat(first.millis()).isLessThan(300);
        assertThat(second.millis()).isLessThan(300);
    }

    @Test
    void ordersFieldValueBeforeExistenceAndExistenceBeforeState() throws Exception {
        expectError(rate(UNKNOWN, "7"), 400, "INVALID_RATING");
        expectError(rate(UNKNOWN, "null"), 400, "INVALID_RATING");
        expectError(rate(UNKNOWN, "4"), 404, "BOOK_NOT_FOUND");
        expectError(call("DELETE", BOOKS + UNKNOWN + "/rating", null), 404, "BOOK_NOT_FOUND");
        assertThat(repository.count()).isEqualTo(3);
    }

    /** S-02 and S-03: leaving completed clears rating and date; completing again sets a new date. */
    @Test
    void leavingCompletedClearsTheRatingAndCompletingAgainStartsWithoutOne() throws Exception {
        rate(DONE_ID, "4");

        Reply reading = call("PUT", BOOKS + DONE_ID + "/status", "{\"status\":\"reading\"}");
        Reply again = call("PUT", BOOKS + DONE_ID + "/status", "{\"status\":\"completed\"}");

        assertThat(tree(reading).path("rating").isNull()).isTrue();
        assertThat(tree(reading).path("date_completed").isNull()).isTrue();
        assertThat(tree(again).path("rating").isNull()).isTrue();
        assertThat(tree(again).path("date_completed").asString()).isNotEqualTo("2026-02-10T09:00:00.000Z");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3F2B8C1E-5A4D-4E6F-9B7A-1C2D3E4F5A6B", "not-a-uuid", "1%27%20OR%20%271%27%3D%271"})
    void malformedIdIsMalformedRequestOnBothRatingMethods(String bad) throws Exception {
        expectError(call("PUT", BOOKS + bad + "/rating", "{\"rating\":4}"), 400, "MALFORMED_REQUEST");
        expectError(call("DELETE", BOOKS + bad + "/rating", null), 400, "MALFORMED_REQUEST");
    }

    /** E-02 rating row and E-29: the other methods are 405 with exactly DELETE, PUT. */
    @Test
    void getOnTheRatingIsMethodNotAllowedWithAllowDeleteAndPut() throws Exception {
        for (String method : new String[] {"GET", "POST", "PATCH"}) {
            Reply reply = call(method, BOOKS + DONE_ID + "/rating", null);
            expectError(reply, 405, "METHOD_NOT_ALLOWED");
            assertThat(reply.allow()).isEqualTo("DELETE, PUT");
            assertThat(reply.type()).isEqualToIgnoringCase("application/json;charset=UTF-8");
        }
    }

    /** E-18: a non-empty body on the clear is MALFORMED_REQUEST; the empty one passes. */
    @Test
    void deleteRatingWithABodyIsMalformedRequestAndWithoutOneIsOk() throws Exception {
        rate(DONE_ID, "3");

        expectError(call("DELETE", BOOKS + DONE_ID + "/rating", " "), 400, "MALFORMED_REQUEST");
        assertThat(tree(call("GET", BOOKS + DONE_ID, null)).path("rating").asInt()).isEqualTo(3);
        assertThat(call("DELETE", BOOKS + DONE_ID + "/rating", null).status()).isEqualTo(200);
    }

    /** C-03 rating step: at 5,000 books a status change then a rating change succeed. */
    @Test
    void atFiveThousandBooksTheRatingStepOfTheCapacityScenarioSucceeds() throws Exception {
        repository.deleteAllInBatch();
        LibrarySeeder.seedShort(repository, 5000);
        String id = LibrarySeeder.idOf(0);

        Reply status = call("PUT", BOOKS + id + "/status", "{\"status\":\"completed\"}");
        Reply rating = rate(id, "4");
        Reply cleared = call("DELETE", BOOKS + id + "/rating", null);

        assertThat(status.status()).isEqualTo(200);
        assertThat(rating.status()).isEqualTo(200);
        assertThat(tree(rating).path("rating").asInt()).isEqualTo(4);
        assertThat(cleared.status()).isEqualTo(200);
        assertThat(repository.count()).isEqualTo(5000);
    }
}
