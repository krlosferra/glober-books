package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.repository.BookRepository;
import com.globant.globerbooks.support.LibrarySeeder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The 2-second add bound (M-2, C-11 a and b, X-04): with 4,999 books of maximum size (500-code-point titles,
 * 200-code-point authors) an add that matches none completes in 2 s with 201, and a duplicate gives
 * {@code 409 DUPLICATE_BOOK} in 2 s, never 500. Time is measured with the monotonic clock around the whole
 * HTTP exchange on a real port, cold (the first request after the seeding).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddBoundIntegrationTest {

    private static final int BOOKS = 4999;
    private static final Duration BOUND = Duration.ofSeconds(2);

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("bound.db"));
    }

    @BeforeEach
    void seedMaximumSizeLibrary() {
        repository.deleteAllInBatch();
        LibrarySeeder.seedMaxSize(repository, BOOKS);
    }

    private record Timed(HttpResponse<String> response, Duration elapsed) {
    }

    private Timed timedAdd(String title, String author) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/books"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"" + title + "\",\"author\":\"" + author + "\"}", StandardCharsets.UTF_8))
                .build();
        long start = System.nanoTime();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        System.out.println("TIMING status=" + response.statusCode() + " elapsed_ms=" + elapsed.toMillis()
                + " body=" + response.body().substring(0, Math.min(90, response.body().length())));
        return new Timed(response, elapsed);
    }

    @Test
    void machineFacts() {
        System.out.println("MACHINE cpus=" + Runtime.getRuntime().availableProcessors() + " os="
                + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " java="
                + System.getProperty("java.version") + " max_heap_mb=" + Runtime.getRuntime().maxMemory() / 1_048_576);
        assertThat(repository.count()).isEqualTo(BOOKS);
    }

    @Test
    void anAddThatMatchesNoneCompletesWithin2SecondsWith201() throws Exception {
        Timed result = timedAdd(LibrarySeeder.maxTitle(90_000), LibrarySeeder.maxAuthor(90_000));

        assertThat(result.response().statusCode()).isEqualTo(201);
        assertThat(result.elapsed()).isLessThan(BOUND);
        assertThat(repository.count()).isEqualTo(BOOKS + 1);
    }

    @Test
    void givesDuplicate409WithinTwoSecondsForFirstMiddleAndLastStoredBook() throws Exception {
        for (int index : new int[] {0, BOOKS / 2, BOOKS - 1}) {
            Timed result = timedAdd(LibrarySeeder.maxTitle(index), LibrarySeeder.maxAuthor(index));

            assertThat(result.response().statusCode()).isEqualTo(409);
            assertThat(result.response().body()).contains("\"code\":\"DUPLICATE_BOOK\"")
                    .contains("\"id\":\"" + LibrarySeeder.idOf(index) + "\"");
            assertThat(result.elapsed()).isLessThan(BOUND);
        }
        assertThat(repository.count()).isEqualTo(BOOKS);
    }

    @Test
    void theTwoStepsOfC11KeepTheBoundAfterTheAddedBookIsRemoved() throws Exception {
        Timed added = timedAdd(LibrarySeeder.maxTitle(90_001), LibrarySeeder.maxAuthor(90_001));
        repository.deleteAllInBatch(repository.findAll().stream()
                .filter(book -> book.getTitle().equals(LibrarySeeder.maxTitle(90_001))).toList());
        Timed duplicate = timedAdd(LibrarySeeder.maxTitle(17), LibrarySeeder.maxAuthor(17));

        assertThat(added.response().statusCode()).isEqualTo(201);
        assertThat(added.elapsed()).isLessThan(BOUND);
        assertThat(duplicate.response().statusCode()).isEqualTo(409);
        assertThat(duplicate.elapsed()).isLessThan(BOUND);
        assertThat(repository.count()).isEqualTo(BOOKS);
    }
}
