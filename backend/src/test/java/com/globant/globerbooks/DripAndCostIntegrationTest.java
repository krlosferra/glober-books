package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.DripClient;
import com.globant.globerbooks.support.DripClient.Result;
import com.globant.globerbooks.support.FreshApp;
import com.globant.globerbooks.support.RawHttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * Re-check of the slow clients (SEC-F-147, R-24) and the normalisation cost at the body cap (SEC-F-153) on the
 * packaged jar (Task 49+50). The drip rows record the real cut time of each variant and whether many drip
 * connections held at once stop the service; they change no connector value. Every wait is a blocking read
 * with a deadline.
 */
class DripAndCostIntegrationTest {

    private static final long WINDOW_MILLIS = 30_000;
    private static final int HELD = 12;
    private static final int MARKS = 8000;
    private static final long NFC_BOUND_MILLIS = 5000;

    @TempDir
    static Path temp;

    private static AppProcess app;
    private static int port;

    @BeforeAll
    static void start() throws Exception {
        app = FreshApp.start(temp);
        port = app.api().port();
    }

    @AfterAll
    static void stop() throws Exception {
        FreshApp.stopAndCheck(app);
    }

    private static Result guarded(int port, String prefix, String dripped, CountDownLatch connected) {
        try {
            return DripClient.run(port, prefix, dripped, WINDOW_MILLIS, connected);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void dripVariantsAreCutAndManyHeldConnectionsDoNotStopTheService() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HELD + 3);
        try {
            dripRun(pool);
        } finally {
            pool.shutdownNow();
        }
    }

    private void dripRun(ExecutorService pool) throws Exception {
        String host = "Host: localhost:" + port + "\r\n";
        String json = "{\"title\":\"Drip title\",\"author\":\"Drip author\"}";
        String postHead = "POST /api/v1/books HTTP/1.1\r\n" + host
                + "Content-Type: application/json\r\nContent-Length: " + json.length() + "\r\n\r\n";
        CompletableFuture<Result> line = CompletableFuture.supplyAsync(() -> guarded(port, "",
                "GET /api/v1/books HTTP/1.1\r\n" + host + "\r\n", null), pool);
        CompletableFuture<Result> headers = CompletableFuture.supplyAsync(() -> guarded(port,
                "GET /api/v1/books HTTP/1.1\r\n", host + "X-Slow: " + "a".repeat(200) + "\r\n\r\n", null),
                pool);
        CompletableFuture<Result> body = CompletableFuture.supplyAsync(() -> guarded(port, postHead, json, null), pool);
        CountDownLatch connected = new CountDownLatch(HELD);
        List<CompletableFuture<Result>> held = new ArrayList<>();
        for (int index = 0; index < HELD; index++) {
            held.add(CompletableFuture.supplyAsync(() -> guarded(port, "",
                    "GET /api/v1/books HTTP/1.1\r\n" + host + "\r\n", connected), pool));
        }
        assertThat(connected.await(30, TimeUnit.SECONDS)).as("all held connections are open").isTrue();
        int duringHold = new RawHttpClient(port).request("GET", "/api/v1/books", null).status();
        new CountDownLatch(1).await(10, TimeUnit.SECONDS);
        int laterInHold = new RawHttpClient(port).request("GET", "/api/v1/books", null).status();
        System.out.println("DRIP request line " + line.get());
        System.out.println("DRIP headers " + headers.get());
        System.out.println("DRIP body " + body.get());
        for (CompletableFuture<Result> one : held) {
            System.out.println("DRIP held request line " + one.get());
        }
        System.out.println("DRIP while " + HELD + " held: normal request status " + duringHold + ", ten seconds later "
                + laterInHold);
        assertThat(body.get().firstLine()).as("the body drip is answered at the total deadline")
                .isEqualTo("HTTP/1.1 400");
        assertThat(duringHold).as("the service answers while drip connections are held").isEqualTo(200);
        assertThat(laterInHold).isEqualTo(200);
        assertThat(new RawHttpClient(port).request("GET", "/api/v1/books", null).status()).isEqualTo(200);
    }

    @Test
    void sec153NormalisationAtTheBodyCapAnswersWithinAGenerousBound() throws Exception {
        int[] descending = {0x0345, 0x0315, 0x0300, 0x0316, 0x0334};
        StringBuilder title = new StringBuilder("x");
        for (int index = 0; index < MARKS; index++) {
            title.appendCodePoint(descending[index % descending.length]);
        }
        String body = JsonMapper.builder().build().writeValueAsString(
                Map.of("title", title.toString(), "author", "Cost author"));
        assertThat(body.getBytes(StandardCharsets.UTF_8).length)
                .as("the request is inside the 16 KiB body cap and carries 8,000 marks").isBetween(16_000, 16_384);
        long begin = System.nanoTime();
        Reply reply = app.api().call("POST", "/api/v1/books", body);
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
        System.out.println("COST NFC 8000 marks | " + reply.label() + " | " + millis + " ms");
        assertThat(reply.status()).isEqualTo(400);
        assertThat(millis).isLessThan(NFC_BOUND_MILLIS);
    }
}
