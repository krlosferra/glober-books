package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.repository.BookRepository;
import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.FaultInjection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
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
 * A failure forced inside the status change by a trigger on the test database (spec S-10, E-21, BR-004,
 * BR-013; security SEC-F-098, SEC-F-140, SEC-F-167, SEC-F-168). The answer is the generic 500, the book is
 * exactly as before, the pool of one connection is not wedged and the application serves again once the
 * trigger is removed. Expected values are copied from the spec.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FaultInjectionIntegrationTest {

    private static final String BOOKS = "/api/v1/books/";
    private static final String DONE_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final String UNREAD_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6d";
    private static final Instant ADDED = Instant.parse("2026-02-01T08:00:00.000Z");
    private static final Instant DONE = Instant.parse("2026-02-10T09:00:00.000Z");
    /** Pool wait of the application (connection-timeout 5000 ms) plus a margin: the next answer must be faster. */
    private static final long POOL_TIMEOUT_MILLIS = 5000;
    private static final int FAILURES = 5;

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    @Autowired
    private DataSource dataSource;

    private ApiCalls api;
    private final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("fault.db"));
    }

    @BeforeEach
    void twoBooks() {
        repository.deleteAllInBatch();
        repository.saveAndFlush(new Book(DONE_ID, "Done", "Author", BookStatus.COMPLETED, 3, ADDED, DONE));
        repository.saveAndFlush(new Book(UNREAD_ID, "Unread", "Author", BookStatus.UNREAD, null, ADDED, null));
        api = new ApiCalls(port);
    }

    @AfterEach
    void removeTrigger() throws Exception {
        FaultInjection.disarm(dataSource);
    }

    private void assertGenericInternalError(Reply reply) {
        assertThat(reply.status()).as(reply.body()).isEqualTo(500);
        JsonNode tree = json.readTree(reply.body());
        assertThat(tree.path("code").asString()).isEqualTo("INTERNAL_ERROR");
        assertThat(tree.path("error").asString()).isEqualTo("Unexpected error.");
        assertThat(tree.has("details")).isFalse();
        assertThat(reply.body().toLowerCase()).doesNotContain(FaultInjection.SECRET_TEXT, "trigger", "raise",
                "sqlite", "select", "update", "exception", "books");
    }

    private Reply timedGet(String id) throws Exception {
        long start = System.nanoTime();
        Reply reply = api.call("GET", BOOKS + id, null);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(millis).as("the pool still serves within its timeout").isLessThan(POOL_TIMEOUT_MILLIS);
        return reply;
    }

    @Test
    void failureAfterTheFirstChangeOfLeavingCompletedGivesGenericErrorAndLeavesTheBookExactlyAsBeforeS10E21()
            throws Exception {
        String before = api.call("GET", BOOKS + DONE_ID, null).body();
        FaultInjection.armAfterStatusChange(dataSource);

        Reply failed = api.call("PUT", BOOKS + DONE_ID + "/status", "{\"status\":\"reading\"}");

        assertGenericInternalError(failed);
        Reply after = timedGet(DONE_ID);
        assertThat(after.status()).isEqualTo(200);
        assertThat(after.body()).isEqualTo(before);
        assertThat(json.readTree(after.body()).path("rating").asInt()).isEqualTo(3);
        assertThat(json.readTree(after.body()).path("date_completed").asString())
                .isEqualTo("2026-02-10T09:00:00.000Z");
        System.out.println("EVIDENCE | S-10 request PUT status reading | " + failed.status() + " | " + failed.body());
        System.out.println("EVIDENCE | S-10 before | " + before);
        System.out.println("EVIDENCE | S-10 after  | " + after.body());
    }

    @Test
    void failureWhileEnteringCompletedLeavesTheUnreadBookWithoutDateOrRating() throws Exception {
        String before = api.call("GET", BOOKS + UNREAD_ID, null).body();
        FaultInjection.armAfterStatusChange(dataSource);

        Reply failed = api.call("PUT", BOOKS + UNREAD_ID + "/status", "{\"status\":\"completed\"}");

        assertGenericInternalError(failed);
        assertThat(timedGet(UNREAD_ID).body()).isEqualTo(before);
    }

    @Test
    void repeatedForcedFailuresDoNotWedgeThePoolAndTheApplicationServesAgainWhenTheTriggerIsRemoved()
            throws Exception {
        String before = api.call("GET", BOOKS + DONE_ID, null).body();
        FaultInjection.armAfterStatusChange(dataSource);
        for (int attempt = 0; attempt < FAILURES; attempt++) {
            assertGenericInternalError(api.call("PUT", BOOKS + DONE_ID + "/status", "{\"status\":\"unread\"}"));
            assertThat(timedGet(DONE_ID).body()).isEqualTo(before);
        }

        FaultInjection.disarm(dataSource);
        Reply recovered = api.call("PUT", BOOKS + DONE_ID + "/status", "{\"status\":\"unread\"}");

        assertThat(recovered.status()).as(recovered.body()).isEqualTo(200);
        JsonNode tree = json.readTree(recovered.body());
        assertThat(tree.path("status").asString()).isEqualTo("unread");
        assertThat(tree.path("rating").isNull()).isTrue();
        assertThat(tree.path("date_completed").isNull()).isTrue();
        System.out.println("EVIDENCE | S-10 after trigger removed | " + recovered.status() + " | " + recovered.body());
    }

    @Test
    void noProductionFileHoldsATestOnlyRouteSwitchOrTriggerA09() throws Exception {
        List<String> forbidden = List.of("create trigger", "raise(", "faultinjection", "appprocess", "x-probe",
                "test-only", "fault.");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(Path.of("src", "main"))) {
            files = walk.filter(Files::isRegularFile).toList();
        }

        assertThat(files).isNotEmpty();
        for (Path file : files) {
            String text = Files.readString(file).toLowerCase();
            for (String marker : forbidden) {
                assertThat(text).as(file + " must not contain " + marker).doesNotContain(marker);
            }
        }
    }
}
