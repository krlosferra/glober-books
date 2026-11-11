package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.domain.Book;
import com.globant.globerbooks.domain.BookStatus;
import com.globant.globerbooks.repository.BookRepository;
import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.CapacityFixtures;
import com.globant.globerbooks.support.LibrarySeeder;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Capacity at maximum size on a real port (Task 41+42, criteria 1 and 2): spec C-05 (the list of 5,000 books
 * of 500-code-point titles and 200-code-point authors outside the basic plane is {@code 200}, {@code total}
 * 5000, below 16,000,000 bytes; the time is recorded, never gated) and C-08 (a loop of 20 add and delete
 * pairs at 4,999 books gives only 201 and 200 and never more than 5,000 books). The mixed load and the heap
 * are in {@code CapacityMixedLoadIntegrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CapacityPerformanceIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final int LIMIT = 5000;
    private static final long BODY_BOUND = 16_000_000L;
    private static final int PAIRS = 20;

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("capacity.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        repository.deleteAllInBatch();
    }

    private void seedWide(int count) {
        List<Book> batch = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            batch.add(new Book(LibrarySeeder.idOf(index), CapacityFixtures.wideTitle(index),
                    CapacityFixtures.wideAuthor(index), BookStatus.UNREAD, null, LibrarySeeder.addedAt(index), null));
        }
        repository.saveAll(batch);
    }

    private String addBody(int index) {
        return "{\"title\":\"" + CapacityFixtures.wideTitle(index) + "\",\"author\":\""
                + CapacityFixtures.wideAuthor(index) + "\"}";
    }

    @Test
    void listOfFiveThousandMaximumSizeBooksIsOkBelowTheBodyBoundAndTheTimeIsRecorded() throws Exception {
        seedWide(LIMIT);
        long start = System.nanoTime();

        CapacityFixtures.ListSize size = CapacityFixtures.fetchList(HttpClient.newHttpClient(), port);

        long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("EVIDENCE C-05 | status=" + size.status() + " body_bytes=" + size.bytes()
                + " elapsed_ms=" + millis + " (recorded, no gate)");
        assertThat(size.status()).isEqualTo(200);
        assertThat(size.bytes()).isLessThan(BODY_BOUND);
        Reply reply = new ApiCalls(port).call("GET", BOOKS, null);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(json.readTree(reply.body()).get("total").asInt()).isEqualTo(LIMIT);
        assertThat(json.readTree(reply.body()).get("books")).hasSize(LIMIT);
    }

    @Test
    void twentyAddAndDeletePairsAt4999BooksGiveOnlyCreatedAndOkAndNeverMoreThan5000Books() throws Exception {
        seedWide(LIMIT - 1);
        ApiCalls api = new ApiCalls(port);
        long longest = 0;

        for (int pair = 0; pair < PAIRS; pair++) {
            long start = System.nanoTime();
            Reply added = api.call("POST", BOOKS, addBody(90_000 + pair));
            longest = Math.max(longest, (System.nanoTime() - start) / 1_000_000);

            assertThat(added.status()).isEqualTo(201);
            assertThat(repository.count()).isEqualTo(LIMIT);
            Reply deleted = api.call("DELETE", BOOKS + "/" + json.readTree(added.body()).get("id").asString(), null);
            assertThat(deleted.status()).isEqualTo(200);
            assertThat(repository.count()).isEqualTo(LIMIT - 1);
        }

        System.out.println("EVIDENCE C-08 | pairs=" + PAIRS + " statuses=201/200 only, max count observed="
                + LIMIT + " longest_add_ms=" + longest);
        assertThat(json.readTree(api.call("GET", BOOKS, null).body()).get("total").asInt()).isEqualTo(LIMIT - 1);
    }
}
