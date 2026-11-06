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
class AddBoundWorstCaseIntegrationTest {

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
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("worst.db"));
    }

    @BeforeEach
    void seedMaximumSizeLibrary() {
        repository.deleteAllInBatch();
        LibrarySeeder.seedMaxSizeSharedAuthor(repository, BOOKS);
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
    void sharedMaximumSizeAuthorKeepsBothAnswersWithin2SecondsColdAndWarm() throws Exception {
        System.out.println("MACHINE cpus=" + Runtime.getRuntime().availableProcessors() + " os="
                + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " java="
                + System.getProperty("java.version"));
        String author = LibrarySeeder.maxAuthor(0);
        for (int round = 0; round < 2; round++) {
            Timed fresh = timedAdd(LibrarySeeder.maxTitle(90_000 + round), author);
            assertThat(fresh.response().statusCode()).isEqualTo(201);
            assertThat(fresh.elapsed()).isLessThan(BOUND);
            repository.deleteById(findAddedId(90_000 + round));
            for (int index : new int[] {0, BOOKS / 2, BOOKS - 1}) {
                Timed result = timedAdd(LibrarySeeder.maxTitle(index), author);

                assertThat(result.response().statusCode()).isEqualTo(409);
                assertThat(result.response().body()).contains("\"code\":\"DUPLICATE_BOOK\"")
                        .contains("\"id\":\"" + LibrarySeeder.idOf(index) + "\"");
                assertThat(result.elapsed()).isLessThan(BOUND);
            }
        }
        assertThat(repository.count()).isEqualTo(BOOKS);
    }

    private String findAddedId(int index) {
        return repository.findAll().stream()
                .filter(book -> book.getTitle().equals(LibrarySeeder.maxTitle(index)))
                .findFirst().orElseThrow().getId();
    }
}
