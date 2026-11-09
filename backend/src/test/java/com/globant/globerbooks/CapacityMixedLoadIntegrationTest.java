package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.CapacityFixtures;
import com.globant.globerbooks.support.SlowReader;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * The mixed load and the memory bound on the packaged jar in a separate JVM with a fixed {@code -Xmx512m}
 * (Task 41+42, criteria 3 and 4; spec C-11 (c), SEC-F-071, SEC-F-081, SEC-F-167). The data location passes the
 * directory guard; the child is stopped in every case. The expected counts are the spec's, the figures
 * (longest request, peak heap) are recorded in the evidence lines.
 */
class CapacityMixedLoadIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final int GROUP = 50;
    private static final int READERS = 8;
    private static final long SLOW_DEADLINE = 12_000;
    private static final long BODY_BOUND = 16_000_000L;

    @TempDir
    Path temp;

    private final JsonMapper json = JsonMapper.builder().build();
    private final ConcurrentLinkedQueue<Long> millis = new ConcurrentLinkedQueue<>();

    private AppProcess start(int books, Path gcLog) throws Exception {
        Path file = CapacityFixtures.maxSizeLibrary(
                CapacityFixtures.guardedData(temp).resolve("library.db"), books, false);
        return AppProcess.start("jdbc:sqlite:" + file, "022", null, temp.resolve("console.log"),
                List.of("-Xlog:gc:file=" + gcLog));
    }

    private Reply timed(Callable<Reply> call) throws Exception {
        long begin = System.nanoTime();
        Reply reply = call.call();
        millis.add((System.nanoTime() - begin) / 1_000_000);
        return reply;
    }

    private static String summary(List<Reply> replies) {
        return replies.stream().collect(Collectors.groupingBy(Reply::label, Collectors.counting())).toString();
    }

    @Test
    void fiftyAddsWithFiftyListsAt4949BooksAllSucceedAndLeave4999WithTheProcessServing() throws Exception {
        Path gcLog = temp.resolve("gc-mixed.log");
        AppProcess app = start(4949, gcLog);
        try {
            ApiCalls api = app.api();
            HttpClient listClient = HttpClient.newHttpClient();
            List<Callable<Reply>> calls = new ArrayList<>();
            for (int index = 0; index < GROUP; index++) {
                String body = "{\"title\":\"" + CapacityFixtures.wideTitle(90_000 + index) + "\",\"author\":\""
                        + CapacityFixtures.wideAuthor(90_000 + index) + "\"}";
                calls.add(() -> timed(() -> api.call("POST", BOOKS, body)));
                calls.add(() -> timed(() -> {
                    CapacityFixtures.ListSize size = CapacityFixtures.fetchList(listClient, api.port());
                    return new Reply(size.status(), "", Long.toString(size.bytes()));
                }));
            }

            List<Reply> replies = ApiCalls.together(calls);

            List<Reply> adds = new ArrayList<>();
            List<Reply> lists = new ArrayList<>();
            for (int index = 0; index < replies.size(); index++) {
                (index % 2 == 0 ? adds : lists).add(replies.get(index));
            }
            long longest = millis.stream().mapToLong(Long::longValue).max().orElseThrow();
            System.out.println("EVIDENCE C-11c | adds=" + summary(adds) + " lists=" + summary(lists)
                    + " longest_request_ms=" + longest + " pool_timeout_ms=5000 requests=" + millis.size());
            System.out.println("EVIDENCE C-11c heap | gc_peak=" + CapacityFixtures.peakHeap(gcLog) + " rss_peak="
                    + CapacityFixtures.peakResidentSet(app.pid()));
            assertThat(adds).allMatch(reply -> reply.status() == 201);
            assertThat(lists).allMatch(reply -> reply.status() == 200
                    && Long.parseLong(reply.body()) < BODY_BOUND);
            assertThat(json.readTree(api.call("GET", BOOKS, null).body()).get("total").asInt()).isEqualTo(4999);
            assertFailedRequestsChangeNothing(api);
            assertThat(app.isAlive()).isTrue();
            assertThat(app.console()).doesNotContain("OutOfMemoryError");
        } finally {
            app.stop();
        }
    }

    private void assertFailedRequestsChangeNothing(ApiCalls api) throws Exception {
        String duplicate = "{\"title\":\"" + CapacityFixtures.wideTitle(0) + "\",\"author\":\""
                + CapacityFixtures.wideAuthor(0) + "\"}";

        Reply refused = api.call("POST", BOOKS, duplicate);
        Reply invalid = api.call("POST", BOOKS, "{\"title\":\"\",\"author\":\"x\"}");

        assertThat(refused.label()).isEqualTo("409 DUPLICATE_BOOK");
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(json.readTree(api.call("GET", BOOKS, null).body()).get("total").asInt()).isEqualTo(4999);
    }

    @Test
    void eightSlowListReadersCauseNoOutOfMemoryAndTheProcessServesAfterwards() throws Exception {
        Path gcLog = temp.resolve("gc-slow.log");
        AppProcess app = start(5000, gcLog);
        CountDownLatch stop = new CountDownLatch(1);
        try {
            int port = app.api().port();
            List<Callable<Reply>> readers = new ArrayList<>();
            for (int index = 0; index < READERS; index++) {
                readers.add(() -> {
                    SlowReader.Outcome outcome =
                            SlowReader.readList(port, 20, SLOW_DEADLINE, stop);
                    return new Reply(200, "", outcome.statusLine().trim() + " bytes=" + outcome.bytes()
                            + " ended=" + outcome.ended());
                });
            }

            List<Reply> outcomes = ApiCalls.together(readers);

            outcomes.forEach(outcome -> System.out.println("EVIDENCE slow reader | " + outcome.body()));
            Reply after = app.api().call("GET", BOOKS, null);
            System.out.println("EVIDENCE slow readers heap | gc_peak=" + CapacityFixtures.peakHeap(gcLog)
                    + " rss_peak=" + CapacityFixtures.peakResidentSet(app.pid()) + " xmx=512m");
            assertThat(app.isAlive()).isTrue();
            assertThat(app.console()).doesNotContain("OutOfMemoryError");
            assertThat(after.status()).isEqualTo(200);
            assertThat(json.readTree(after.body()).get("total").asInt()).isEqualTo(5000);
        } finally {
            stop.countDown();
            app.stop();
        }
    }
}
