package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.repository.BookRepository;
import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.LibrarySeeder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port tests of simultaneous adds (spec C-06, C-07, C-09, C-10; BR-003, BR-009, BR-013). Every group of
 * requests is released together by a barrier. The expected counts are the ones the spec states, not the ones
 * observed. Answers are summarised in the evidence lines.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final int LIMIT = 5000;

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository repository;

    private ApiCalls api;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("concurrency.db"));
    }

    @BeforeEach
    void emptyLibrary() {
        repository.deleteAllInBatch();
        api = new ApiCalls(port);
    }

    private List<Reply> addTogether(int count, String titlePrefix, boolean sameBook, String extra) throws Exception {
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String title = sameBook ? titlePrefix : titlePrefix + " " + index;
            String body = "{\"title\":\"" + title + "\",\"author\":\"Concurrent Author\"" + extra + "}";
            calls.add(() -> api.call("POST", BOOKS, body));
        }
        List<Reply> replies = ApiCalls.together(calls);
        System.out.println("EVIDENCE | " + titlePrefix + " x" + count + " | " + summary(replies));
        return replies;
    }

    private static String summary(List<Reply> replies) {
        return replies.stream().collect(Collectors.groupingBy(Reply::label, Collectors.counting())).toString();
    }

    private static long count(List<Reply> replies, int status, String code) {
        return replies.stream().filter(reply -> reply.status() == status && reply.code().equals(code)).count();
    }

    @Test
    void twoSimultaneousAddsAt4999BooksGiveOneCreatedAndOneLibraryFull() throws Exception {
        LibrarySeeder.seedShort(repository, LIMIT - 1);

        List<Reply> replies = addTogether(2, "Last seat", false, "");

        assertThat(count(replies, 201, "")).isEqualTo(1);
        assertThat(count(replies, 409, "LIBRARY_FULL")).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(LIMIT);
    }

    @Test
    void twoSimultaneousIdenticalAddsGiveOneCreatedAndOneDuplicateAndOneBookStored() throws Exception {
        List<Reply> replies = addTogether(2, "Same book", true, "");

        assertThat(count(replies, 201, "")).isEqualTo(1);
        assertThat(count(replies, 409, "DUPLICATE_BOOK")).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void twentyAddsAt4990BooksGiveExactlyTenCreatedAndTenLibraryFull() throws Exception {
        LibrarySeeder.seedShort(repository, LIMIT - 10);

        List<Reply> replies = addTogether(20, "Place", false, "");

        assertThat(count(replies, 201, "")).isEqualTo(10);
        assertThat(count(replies, 409, "LIBRARY_FULL")).isEqualTo(10);
        assertThat(repository.count()).isEqualTo(LIMIT);
    }

    @Test
    void twentyIdenticalAddsGiveOneCreatedAndNineteenDuplicatesThenTwentyCreatedWithTheOverride() throws Exception {
        List<Reply> withoutOverride = addTogether(20, "Twin", true, "");

        assertThat(count(withoutOverride, 201, "")).isEqualTo(1);
        assertThat(count(withoutOverride, 409, "DUPLICATE_BOOK")).isEqualTo(19);
        assertThat(repository.count()).isEqualTo(1);

        List<Reply> withOverride = addTogether(20, "Twin", true, ",\"allow_duplicate\":true");

        assertThat(count(withOverride, 201, "")).isEqualTo(20);
        assertThat(repository.count()).isEqualTo(21);
    }
}
