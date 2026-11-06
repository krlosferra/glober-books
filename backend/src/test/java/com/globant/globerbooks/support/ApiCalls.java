package com.globant.globerbooks.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.json.JsonMapper;

/**
 * Small HTTP client for the real-port tests of concurrency and durability: one request per call over
 * HTTP/1.1, and a runner that releases a group of requests together through a barrier (never a sleep).
 */
public final class ApiCalls {

    /** Longest wait for any group of requests, in seconds. */
    private static final int GROUP_SECONDS = 120;

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String base;

    /**
     * Creates a client for one loopback port.
     *
     * @param port the port of the application under test
     */
    public ApiCalls(int port) {
        this.base = "http://127.0.0.1:" + port;
    }

    /** One answer: status, error code (empty when the body has none) and the raw body. */
    public record Reply(int status, String code, String body) {

        /** Short text for assertion messages and evidence. */
        public String label() {
            return status + (code.isEmpty() ? "" : " " + code);
        }
    }

    /**
     * Sends one request with the JSON content type.
     *
     * @param method the HTTP method
     * @param path the path below the host
     * @param body the body text, or null for none
     * @return the answer
     * @throws Exception when the transport fails
     */
    public Reply call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String text = response.body();
        String code = "";
        if (response.statusCode() >= 400) {
            code = json.readTree(text).path("code").asString();
        }
        return new Reply(response.statusCode(), code, text);
    }

    /**
     * Runs the calls on separate threads, all released together by a barrier, and returns the answers
     * in the order of the calls.
     *
     * @param calls the requests to send together
     * @return the answers, one per call
     * @throws Exception when a call fails or the group does not finish in time
     */
    public static List<Reply> together(List<Callable<Reply>> calls) throws Exception {
        CyclicBarrier gate = new CyclicBarrier(calls.size());
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        try {
            List<Future<Reply>> futures = new ArrayList<>();
            for (Callable<Reply> call : calls) {
                futures.add(pool.submit(() -> {
                    gate.await(GROUP_SECONDS, TimeUnit.SECONDS);
                    return call.call();
                }));
            }
            List<Reply> replies = new ArrayList<>();
            for (Future<Reply> future : futures) {
                replies.add(future.get(GROUP_SECONDS, TimeUnit.SECONDS));
            }
            return replies;
        } finally {
            pool.shutdownNow();
        }
    }
}
