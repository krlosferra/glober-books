package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.CapacityFixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * The cold 2-second figures of SEC-F-166 on a fresh JVM (Task 41+42; spec M-2, C-11 a and b): with 4,999
 * books of maximum size that all share one maximum-size author (the worst case of the duplicate scan), the
 * first add and the first duplicates of the first, middle and last book each answer within 2 s. The start-up
 * check of the test harness is one list request, so the add and duplicate paths themselves are cold. Two
 * fresh JVMs are used: one whose first mutating request is the add, one whose first is a duplicate.
 */
class ColdStartBoundIntegrationTest {

    private static final int BOOKS = 4999;
    private static final Duration BOUND = Duration.ofSeconds(2);

    private final JsonMapper json = JsonMapper.builder().build();

    @TempDir
    Path temp;

    private String body(int titleIndex, int authorIndex) {
        return "{\"title\":\"" + CapacityFixtures.wideTitle(titleIndex) + "\",\"author\":\""
                + CapacityFixtures.wideAuthor(authorIndex) + "\"}";
    }

    private AppProcess fresh(String run) throws Exception {
        Path parent = Files.createDirectory(temp.resolve(run));
        Path file = CapacityFixtures.maxSizeLibrary(
                CapacityFixtures.guardedData(parent).resolve("library.db"), BOOKS, true);
        return AppProcess.start("jdbc:sqlite:" + file, "022", null, parent.resolve("console.log"));
    }

    private long timed(String label, ApiCalls api, String requestBody, int status, String code) throws Exception {
        long begin = System.nanoTime();
        Reply reply = api.call("POST", "/api/v1/books", requestBody);
        long millis = (System.nanoTime() - begin) / 1_000_000;
        System.out.println("EVIDENCE cold | " + label + " | " + reply.label() + " | " + millis + " ms");
        assertThat(reply.status()).isEqualTo(status);
        assertThat(reply.code()).isEqualTo(code);
        assertThat(Duration.ofMillis(millis)).isLessThan(BOUND);
        return millis;
    }

    @Test
    void firstAddOnAFreshJvmThenDuplicatesOfFirstMiddleAndLastAnswerWithin2Seconds() throws Exception {
        System.out.println("MACHINE cpus=" + Runtime.getRuntime().availableProcessors() + " os="
                + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " java="
                + System.getProperty("java.version"));
        AppProcess app = fresh("add");
        try {
            ApiCalls api = app.api();
            timed("fresh JVM, first add", api, body(90_000, 0), 201, "");
            // At 5,000 books LIBRARY_FULL is answered before the duplicate check (spec table of Section 5.0):
            // delete the added book so that the library is at 4,999 again, the costly case.
            Reply added = api.call("GET", "/api/v1/books", null);
            String addedId = json.readTree(added.body()).get("books").get(0).get("id").asString();
            assertThat(json.readTree(added.body()).get("books").get(0).get("title").asString())
                    .isEqualTo(CapacityFixtures.wideTitle(90_000));
            assertThat(api.call("DELETE", "/api/v1/books/" + addedId, null).status()).isEqualTo(200);
            for (int index : new int[] {0, BOOKS / 2, BOOKS - 1}) {
                timed("duplicate of book " + index, api, body(index, 0), 409, "DUPLICATE_BOOK");
            }
        } finally {
            app.stop();
        }
    }

    @Test
    void eachFirstRequestOfAFreshJvmBeingADuplicateAnswersWithin2SecondsForFirstMiddleAndLast() throws Exception {
        for (int index : new int[] {0, BOOKS / 2, BOOKS - 1}) {
            AppProcess app = fresh("duplicate" + index);
            try {
                timed("fresh JVM, first request is the duplicate of book " + index, app.api(), body(index, 0),
                        409, "DUPLICATE_BOOK");
            } finally {
                app.stop();
            }
        }
    }
}
