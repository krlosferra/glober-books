package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.repository.BookRepository;
import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port races on one book (spec S-08, S-09, BR-004, BR-013, DEC-018; security SEC-F-177). Each group of
 * requests is released together by a barrier. The tests assert the set of allowed answers and a final state
 * allowed by INV-1 to INV-4, never an interleaving.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyRaceIntegrationTest {

    private static final String BOOKS = "/api/v1/books/";
    private static final Instant ADDED = Instant.parse("2026-02-01T08:00:00.000Z");
    private static final Instant DONE = Instant.parse("2026-02-10T09:00:00.000Z");
    private static final int SPEC_ITERATIONS = 20;
    private static final int RACE_ITERATIONS = 60;

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private ApiCalls api;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("races.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        repository.deleteAllInBatch();
        api = new ApiCalls(port);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private void storeCompleted(String id, Integer rating) {
        repository.saveAndFlush(new Book(id, "Race title", "Race author", BookStatus.COMPLETED, rating, ADDED, DONE));
    }

    private Callable<Reply> status(String id, String wire) {
        return () -> api.call("PUT", BOOKS + id + "/status", "{\"status\":\"" + wire + "\"}");
    }

    private Callable<Reply> rate(String id, int value) {
        return () -> api.call("PUT", BOOKS + id + "/rating", "{\"rating\":" + value + "}");
    }

    private static void tally(Map<String, Integer> counts, String name, Reply reply) {
        counts.merge(name + " " + reply.label(), 1, Integer::sum);
    }

    /** INV-1 to INV-4 for one stored book. */
    private static void assertInvariants(Book book) {
        assertThat(Set.of(BookStatus.UNREAD, BookStatus.READING, BookStatus.COMPLETED)).contains(book.getStatus());
        book.getRating().ifPresent(value -> {
            assertThat(book.getStatus()).as("INV-1").isEqualTo(BookStatus.COMPLETED);
            assertThat(value).as("INV-2").isBetween(1, 5);
        });
        assertThat(book.getDateCompleted().isPresent()).as("INV-4")
                .isEqualTo(book.getStatus() == BookStatus.COMPLETED);
    }

    @Test
    void statusReadingAndRatingFourTogetherAlwaysEndReadingWithoutRatingOrDateS08() throws Exception {
        Map<String, Integer> counts = new TreeMap<>();
        for (int iteration = 0; iteration < SPEC_ITERATIONS; iteration++) {
            String id = newId();
            storeCompleted(id, 3);

            List<Reply> replies = ApiCalls.together(List.of(status(id, "reading"), rate(id, 4)));

            Reply statusReply = replies.get(0);
            Reply ratingReply = replies.get(1);
            assertThat(statusReply.status()).as(statusReply.body()).isEqualTo(200);
            assertThat(ratingReply.label()).as(ratingReply.body()).isIn("200", "400 RATING_NOT_ALLOWED");
            Book stored = repository.findById(id).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(BookStatus.READING);
            assertThat(stored.getRating()).isEmpty();
            assertThat(stored.getDateCompleted()).isEmpty();
            assertInvariants(stored);
            tally(counts, "status", statusReply);
            tally(counts, "rating", ratingReply);
        }
        System.out.println("EVIDENCE | S-08 " + SPEC_ITERATIONS + " iterations | " + counts);
    }

    @Test
    void statusCompletedRatingClearAndDeleteTogetherAnswerOnlyOkOrNotFoundAndKeepInvariantsS09() throws Exception {
        Map<String, Integer> counts = new TreeMap<>();
        Map<String, Integer> finals = new TreeMap<>();
        for (int iteration = 0; iteration < SPEC_ITERATIONS; iteration++) {
            String id = newId();
            repository.saveAndFlush(new Book(id, "Race title", "Race author", BookStatus.READING, null, ADDED, null));
            Callable<Reply> clear = () -> api.call("DELETE", BOOKS + id + "/rating", null);
            Callable<Reply> remove = () -> api.call("DELETE", BOOKS + id, null);

            List<Reply> replies = ApiCalls.together(List.of(status(id, "completed"), clear, remove));

            String[] names = {"status", "clear", "delete"};
            for (int index = 0; index < replies.size(); index++) {
                Reply reply = replies.get(index);
                assertThat(reply.label()).as(names[index] + " " + reply.body()).isIn("200", "404 BOOK_NOT_FOUND");
                tally(counts, names[index], reply);
            }
            Optional<Book> stored = repository.findById(id);
            stored.ifPresent(ConcurrencyRaceIntegrationTest::assertInvariants);
            finals.merge(stored.map(book -> book.getStatus().name()).orElse("ABSENT"), 1, Integer::sum);
        }
        System.out.println("EVIDENCE | S-09 " + SPEC_ITERATIONS + " iterations | " + counts + " | final " + finals);
    }

    @Test
    void ratingAgainstEveryStatusChangeOnOneCompletedBookKeepsInvariantsSecF177() throws Exception {
        String id = newId();
        String[] targets = {"reading", "unread", "completed"};
        Map<String, Integer> counts = new TreeMap<>();
        Map<String, Integer> finals = new TreeMap<>();
        for (int iteration = 0; iteration < RACE_ITERATIONS; iteration++) {
            String target = targets[iteration % targets.length];
            storeCompleted(id, 3);

            List<Reply> replies = ApiCalls.together(List.of(status(id, target), rate(id, 4)));

            assertThat(replies.get(0).status()).as(replies.get(0).body()).isEqualTo(200);
            assertThat(replies.get(1).label()).as(replies.get(1).body()).isIn("200", "400 RATING_NOT_ALLOWED");
            Book stored = repository.findById(id).orElseThrow();
            assertInvariants(stored);
            assertThat(stored.getStatus().name().toLowerCase()).isEqualTo(target);
            tally(counts, "to " + target + " status", replies.get(0));
            tally(counts, "to " + target + " rating", replies.get(1));
            finals.merge(target + " rating=" + stored.getRating().map(String::valueOf).orElse("null"), 1,
                    Integer::sum);
        }
        System.out.println("EVIDENCE | SEC-F-177 " + RACE_ITERATIONS + " iterations | " + counts
                + " | final " + finals);
    }
}
