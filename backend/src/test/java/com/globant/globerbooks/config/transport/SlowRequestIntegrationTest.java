package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Live slow-request tests (spec E-22; tasks.md Task 16 criterion 5; SEC-F-073) on a real port. No
 * sleep is used to synchronise: every wait is a blocking read with a timeout, and the drip is paced
 * by a scheduler.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SlowRequestIntegrationTest {

    private static final Duration DRIP_WINDOW = Duration.ofSeconds(20);

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("slow.db"));
    }

    /** Result of a connection watch: whether the server closed it and after how long. */
    private record Watch(boolean closed, long millis, long firstByteMillis, String firstLine) {
    }

    /** Reads until the server closes the socket or the limit passes; returns what happened. */
    private Watch watch(Socket socket, Duration limit) throws IOException {
        long start = System.nanoTime();
        socket.setSoTimeout((int) limit.toMillis());
        InputStream input = socket.getInputStream();
        StringBuilder seen = new StringBuilder();
        boolean closed = false;
        long firstByte = -1;
        try {
            int next = input.read();
            while (next >= 0) {
                if (firstByte < 0) {
                    firstByte = Duration.ofNanos(System.nanoTime() - start).toMillis();
                }
                seen.append((char) next);
                next = input.read();
            }
            closed = true;
        } catch (SocketTimeoutException stillOpen) {
            closed = false;
        } catch (IOException reset) {
            closed = true;
        }
        String first = seen.toString().split("\r\n", 2)[0].trim();
        return new Watch(closed, Duration.ofNanos(System.nanoTime() - start).toMillis(), firstByte, first);
    }

    @Test
    void e22IdleConnectionIsClosedWithinTheTimeoutWhileOthersAreServed() throws Exception {
        try (Socket idle = new Socket(InetAddress.getLoopbackAddress(), port)) {
            idle.getOutputStream().write(("GET /api/v1/books HTTP/1.1\r\nHost: localhost:" + port + "\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            idle.getOutputStream().flush();

            Answer other = new RawHttpClient(port).request("GET", "/api/v1/books", null);
            Watch result = watch(idle, Duration.ofSeconds(9));
            System.out.println("EVIDENCE E-22 closed=" + result.closed() + " after ms " + result.millis()
                    + " first line [" + result.firstLine() + "]");

            assertThat(other.status()).isEqualTo(200);
            assertThat(result.closed()).isTrue();
            assertThat(result.millis()).isLessThan(8_000L);
        }
    }

    @Test
    void dripVariantsAreRunAndRecorded() throws Exception {
        String head = "POST /api/v1/books HTTP/1.1\r\nHost: localhost:" + port
                + "\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n";
        String get = "GET /api/v1/books HTTP/1.1\r\nHost: localhost:" + port + "\r\nAccept: */*\r\n\r\n";
        String rest = "Host: localhost:" + port + "\r\nX-Slow: " + "a".repeat(200) + "\r\n\r\n";
        CompletableFuture<Watch> line = CompletableFuture.supplyAsync(() -> run("", get));
        CompletableFuture<Watch> headers = CompletableFuture.supplyAsync(
                () -> run("GET /api/v1/books HTTP/1.1\r\n", rest));
        CompletableFuture<Watch> body = CompletableFuture.supplyAsync(
                () -> run(head, "{\"title\":\"" + "a".repeat(80) + "\"}"));
        Watch bodyResult = body.get();
        System.out.println("EVIDENCE DRIP request line (accepted limit, SEC-F-073) " + line.get());
        System.out.println("EVIDENCE DRIP headers (accepted limit, SEC-F-073) " + headers.get());
        System.out.println("EVIDENCE DRIP body (total deadline) " + bodyResult);

        assertThat(bodyResult.firstLine()).isEqualTo("HTTP/1.1 400");
        assertThat(bodyResult.firstByteMillis()).isBetween(9_000L, 16_000L);
    }

    private Watch run(String prefix, String dripped) {
        try {
            return drip(prefix, dripped);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private Watch drip(String prefix, String dripped) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.getOutputStream().write(prefix.getBytes(StandardCharsets.ISO_8859_1));
            byte[] bytes = dripped.getBytes(StandardCharsets.ISO_8859_1);
            AtomicBoolean stop = new AtomicBoolean();
            int[] position = {0};
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!stop.get() && position[0] < bytes.length) {
                        socket.getOutputStream().write(bytes[position[0]++]);
                        socket.getOutputStream().flush();
                    }
                } catch (IOException closed) {
                    stop.set(true);
                }
            }, 2, 2, TimeUnit.SECONDS);
            Watch result = watch(socket, DRIP_WINDOW);
            stop.set(true);
            return result;
        } finally {
            scheduler.shutdownNow();
        }
    }
}
