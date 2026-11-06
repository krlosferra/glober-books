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

/**
 * Real-port tests of the add rules (BR-003, BR-009, US-001.7, US-004, C-01, C-02, T-12, T-40) on a
 * temporary SQLite file. Expected values are copied from the spec; the library is seeded through the
 * repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddRulesIntegrationTest {

    private static final int CAPACITY = 5000;
    private static final String FULL_BODY_PATTERN =
            "\\{\"error\":\"[^\"]*\",\"code\":\"LIBRARY_FULL\",\"timestamp\":\"[0-9TZ:.-]{24}\"\\}";
    private static final Instant T1 = Instant.parse("2026-02-01T00:00:00.000Z");

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("rules.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        repository.deleteAllInBatch();
    }

    private HttpResponse<String> call(String method, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/books"));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String excerpt = response.body().length() > 160 ? response.body().substring(0, 160) + "..." : response.body();
        System.out.println("EVIDENCE " + method + " " + (body == null ? "" : body.length() > 80
                ? body.substring(0, 80) + "..." : body) + " -> " + response.statusCode() + " " + excerpt);
        return response;
    }

    private HttpResponse<String> add(String title, String author, String extra) throws Exception {
        return call("POST", "{\"title\":\"" + title + "\",\"author\":\"" + author + "\"" + extra + "}");
    }

    private void store(String id, String title, String author, BookStatus status, Integer rating, Instant added) {
        Instant completed = status == BookStatus.COMPLETED ? added : null;
        repository.save(new Book(id, title, author, status, rating, added, completed));
    }

    @Test
    void duplicateGives409WithTheExistingBookAndStoresNothing() throws Exception {
        store("00000000-0000-4000-8000-0000000000aa", "Clean Code", "Robert Martin", BookStatus.UNREAD, null, T1);

        HttpResponse<String> response = add(" clean   code ", "ROBERT MARTIN", "");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).matches("\\{\"error\":\"[^\"]*\",\"code\":\"DUPLICATE_BOOK\","
                + "\"timestamp\":\"[0-9TZ:.-]{24}\",\"details\":\\{\"existing_book\":\\{"
                + "\"id\":\"00000000-0000-4000-8000-0000000000aa\",\"title\":\"Clean Code\","
                + "\"author\":\"Robert Martin\",\"status\":\"unread\",\"rating\":null,"
                + "\"date_added\":\"2026-02-01T00:00:00.000Z\",\"date_completed\":null\\}\\}\\}");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void overrideTrueStoresASeparateBookAndFalseOrAbsentDoesNot() throws Exception {
        store("00000000-0000-4000-8000-0000000000aa", "Clean Code", "Robert Martin", BookStatus.UNREAD, null, T1);

        assertThat(add("Clean Code", "Robert Martin", ",\"allow_duplicate\":false").statusCode()).isEqualTo(409);
        assertThat(add("Clean Code", "Robert Martin", "").statusCode()).isEqualTo(409);
        HttpResponse<String> override = add("Clean Code", "Robert Martin", ",\"allow_duplicate\":true");

        assertThat(override.statusCode()).isEqualTo(201);
        assertThat(override.body()).doesNotContain("00000000-0000-4000-8000-0000000000aa");
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void statusRatingAndDatesDoNotAffectDetection() throws Exception {
        store("00000000-0000-4000-8000-0000000000aa", "Done Book", "Ann", BookStatus.COMPLETED, 5, T1);
        store("00000000-0000-4000-8000-0000000000bb", "Reading Book", "Ann", BookStatus.READING, null, T1);

        HttpResponse<String> first = add("done book", "ann", ",\"status\":\"unread\"");
        HttpResponse<String> second = add("READING BOOK", "ANN", ",\"status\":\"completed\"");

        assertThat(first.statusCode()).isEqualTo(409);
        assertThat(first.body()).contains("\"status\":\"completed\",\"rating\":5");
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void reportsTheEarliestMatchAndTheLowestIdOnATie() throws Exception {
        store("00000000-0000-4000-8000-0000000000cc", "Twin", "Ann", BookStatus.UNREAD, null, T1.plusSeconds(60));
        store("00000000-0000-4000-8000-0000000000bb", "TWIN", "ann", BookStatus.UNREAD, null, T1);
        store("00000000-0000-4000-8000-0000000000aa", "Twin ", "Ann", BookStatus.UNREAD, null, T1);

        HttpResponse<String> response = add("twin", "ANN", "");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"existing_book\":{\"id\":\"00000000-0000-4000-8000-0000000000aa\"");
    }

    @Test
    void sharpSAndDoubleSBothStoreAndCaseVariantsOfOneAuthorDuplicate() throws Exception {
        assertThat(add("Stra\\u00dfe", "a", "").statusCode()).isEqualTo(201);
        assertThat(add("STRASSE", "a", "").statusCode()).isEqualTo(201);
        assertThat(add("Clean Code", "b", "").statusCode()).isEqualTo(201);
        HttpResponse<String> duplicate = add("CLEAN CODE", "b", "");

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("\"code\":\"DUPLICATE_BOOK\"");
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void injectionPayloadsAreStoredUnchangedAndTheCountGrowsByOneEachTime() throws Exception {
        String[] titles = {"'; DROP TABLE books;--", "<script>alert(1)</script>", "${7*7}", "#{7*7}", "%s%n"};
        for (int index = 0; index < titles.length; index++) {
            assertThat(add(titles[index], "Mallory", "").statusCode()).isEqualTo(201);
            assertThat(repository.count()).isEqualTo(index + 1);
        }

        assertThat(repository.findAll()).extracting(Book::getTitle).containsExactlyInAnyOrder(titles);
    }

    @Test
    void atCapacityEveryAddGives409LibraryFullBeforeTheDuplicateCheckAndTheCountStays() throws Exception {
        LibrarySeeder.seedShort(repository, CAPACITY);
        String author = "Seed author 7";
        String title = LibrarySeeder.shortTitle(7);

        HttpResponse<String> fresh = add("Brand new", "Someone", "");
        HttpResponse<String> duplicateOverride = add(title, author, ",\"allow_duplicate\":true");
        HttpResponse<String> duplicatePlain = add(title, author, "");
        HttpResponse<String> freshOverride = add("Brand new", "Someone", ",\"allow_duplicate\":true");

        for (HttpResponse<String> response : java.util.List.of(fresh, duplicateOverride, duplicatePlain,
                freshOverride)) {
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).matches(FULL_BODY_PATTERN);
        }
        assertThat(repository.count()).isEqualTo(CAPACITY);
    }

    @Test
    void the5000thBookIsAcceptedAndThenTheLibraryIsFull() throws Exception {
        LibrarySeeder.seedShort(repository, CAPACITY - 1);

        assertThat(add("The last one", "Someone", "").statusCode()).isEqualTo(201);
        assertThat(add("One too many", "Someone", "").statusCode()).isEqualTo(409);
        assertThat(repository.count()).isEqualTo(CAPACITY);
    }
}
