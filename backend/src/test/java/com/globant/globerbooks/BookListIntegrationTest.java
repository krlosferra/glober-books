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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real-port tests of the list and its {@code status} filter (US-002.4, .5, .8, BR-010). The expected
 * order is newest {@code date_added} first, ties by {@code id} ascending; the library is seeded through the
 * repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookListIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-02-01T00:00:00.000Z");
    private static final Instant T2 = Instant.parse("2026-02-02T00:00:00.000Z");
    private static final Instant T3 = Instant.parse("2026-02-03T00:00:00.000Z");

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
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("list.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        repository.deleteAllInBatch();
    }

    private HttpResponse<String> get(String query) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + port + "/api/v1/books" + query);
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = response.body();
        System.out.println("EVIDENCE GET " + query + " -> " + response.statusCode() + " "
                + (body.length() > 160 ? body.substring(0, 160) + "..." : body));
        return response;
    }

    private void store(String id, BookStatus status, Instant added) {
        Instant completed = status == BookStatus.COMPLETED ? added : null;
        Integer rating = status == BookStatus.COMPLETED ? 4 : null;
        String title = "Title " + id.substring(id.length() - 2);
        repository.save(new Book(id, title, "Author", status, rating, added, completed));
    }

    private List<String> idsOf(HttpResponse<String> response) throws Exception {
        List<String> ids = new ArrayList<>();
        json.readTree(response.body()).get("books").forEach(node -> ids.add(node.get("id").asString()));
        return ids;
    }

    private void seedMixed() {
        store("00000000-0000-4000-8000-0000000000a1", BookStatus.READING, T1);
        store("00000000-0000-4000-8000-0000000000b2", BookStatus.UNREAD, T3);
        store("00000000-0000-4000-8000-0000000000c3", BookStatus.READING, T3);
        store("00000000-0000-4000-8000-0000000000d4", BookStatus.COMPLETED, T2);
        store("00000000-0000-4000-8000-0000000000e5", BookStatus.READING, T2);
    }

    @Test
    void unfilteredListIsNewestFirstWithTiesByIdAndTotalOfAll() throws Exception {
        seedMixed();

        HttpResponse<String> response = get("");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(idsOf(response)).containsExactly(
                "00000000-0000-4000-8000-0000000000b2", "00000000-0000-4000-8000-0000000000c3",
                "00000000-0000-4000-8000-0000000000d4", "00000000-0000-4000-8000-0000000000e5",
                "00000000-0000-4000-8000-0000000000a1");
        assertThat(json.readTree(response.body()).get("total").asInt()).isEqualTo(5);
    }

    @Test
    void statusFilterKeepsOnlyThatStatusInTheSameOrderWithTheFilteredTotal() throws Exception {
        seedMixed();

        HttpResponse<String> response = get("?status=reading");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(idsOf(response)).containsExactly("00000000-0000-4000-8000-0000000000c3",
                "00000000-0000-4000-8000-0000000000e5", "00000000-0000-4000-8000-0000000000a1");
        JsonNode root = json.readTree(response.body());
        assertThat(root.get("total").asInt()).isEqualTo(3);
        assertThat(root.propertyNames()).containsExactlyInAnyOrder("books", "total");
        assertThat(get("?status=completed").body()).contains("\"total\":1");
        assertThat(get("?status=unread").body()).contains("\"total\":1");
    }

    @Test
    void decodesAPercentEncodedValueBeforeTheFilter() throws Exception {
        seedMixed();

        HttpResponse<String> response = get("?status=%72eading");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).get("total").asInt()).isEqualTo(3);
    }

    @Test
    void emptyLibraryAndEmptyFilterGive200WithAnEmptyArrayAndZero() throws Exception {
        assertThat(get("").body()).isEqualTo("{\"books\":[],\"total\":0}");
        assertThat(get("?status=reading").body()).isEqualTo("{\"books\":[],\"total\":0}");
        store("00000000-0000-4000-8000-0000000000a1", BookStatus.UNREAD, T1);

        HttpResponse<String> response = get("?status=completed");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"books\":[],\"total\":0}");
    }

    @Test
    void anInvalidStatusValueIsStillRejectedBeforeTheList() throws Exception {
        seedMixed();

        HttpResponse<String> response = get("?status=Reading");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"INVALID_STATUS\"");
    }

    @Test
    void listsAtMost5000EntriesOfAFullLibraryWithoutPaginationMembers() throws Exception {
        LibrarySeeder.seedShort(repository, 5000);

        HttpResponse<String> response = get("");

        JsonNode root = json.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(root.get("books").size()).isEqualTo(5000);
        assertThat(root.get("total").asInt()).isEqualTo(5000);
        assertThat(root.propertyNames()).containsExactlyInAnyOrder("books", "total");
        assertThat(get("?status=unread").body()).contains("\"total\":5000");
    }
}
