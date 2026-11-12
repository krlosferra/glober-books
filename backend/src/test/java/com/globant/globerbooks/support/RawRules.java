package com.globant.globerbooks.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Shared checks for the raw-socket rows of Task 49+50: what an answer given before the application may
 * disclose (SC-29, R-13, R-23), how a body is classified for the evidence table, and the proof that the next
 * request on a new connection is served.
 */
public final class RawRules {

    private static final List<String> SHOWN_HEADERS = List.of("Content-Type", "Content-Security-Policy",
            "X-Content-Type-Options", "Cache-Control", "Server", "Allow");

    private final RawHttpClient client;

    /**
     * Creates the checks for one running application.
     *
     * @param port the loopback port
     */
    public RawRules(int port) {
        this.client = new RawHttpClient(port);
    }

    /**
     * Returns the client behind the checks.
     *
     * @return the raw client
     */
    public RawHttpClient client() {
        return client;
    }

    /**
     * Builds a request head with the allowed Host (unless a header line gives one) and the connection closed.
     *
     * @param requestLine the request line without CRLF
     * @param port the loopback port
     * @param headers header lines without CRLF
     * @return the head ending with the blank line
     */
    public static String head(String requestLine, int port, String... headers) {
        StringBuilder text = new StringBuilder(requestLine).append("\r\n");
        boolean hasHost = false;
        for (String header : headers) {
            hasHost |= header.toLowerCase(Locale.ROOT).startsWith("host:");
            text.append(header).append("\r\n");
        }
        if (!hasHost) {
            text.append("Host: localhost:").append(port).append("\r\n");
        }
        return text.append("Connection: close\r\n\r\n").toString();
    }

    /**
     * Classifies a body for the evidence table.
     *
     * @param answer the answer
     * @return "empty", "json-envelope", "html" or "other"
     */
    public static String bodyClass(Answer answer) {
        String body = answer.body().trim();
        if (body.isEmpty()) {
            return "empty";
        }
        if (body.startsWith("{") && body.contains("\"code\"")) {
            return "json-envelope";
        }
        return body.toLowerCase(Locale.ROOT).contains("<html") || body.startsWith("<!") ? "html" : "other";
    }

    /**
     * Asserts that an answer names no product or version and echoes none of the planted fragments.
     *
     * @param answer the answer
     * @param echoes fragments of the request that must not come back
     */
    public static void assertNoDisclosure(Answer answer, String... echoes) {
        assertThat(answer.raw()).as("answer text").isNotEmpty()
                .doesNotContainIgnoringCase("tomcat").doesNotContainIgnoringCase("apache")
                .doesNotContainIgnoringCase("coyote").doesNotContainIgnoringCase("spring")
                .doesNotContainIgnoringCase("java").doesNotContainIgnoringCase("jetty")
                .doesNotContainIgnoringCase("x-powered-by").doesNotContainPattern("\\d+\\.\\d+\\.\\d+");
        assertThat(answer.header("Server")).as("Server header").isNull();
        for (String echo : echoes) {
            assertThat(answer.raw()).as("echo of " + echo).doesNotContain(echo);
        }
    }

    /**
     * Prints one evidence line: the request, the status, the body class, the headers present and whether the
     * next request on a new connection was served.
     *
     * @param label the row label
     * @param answer the answer
     * @return whether the next request (the book list on a new connection) answered 200
     * @throws IOException on a socket failure
     */
    public boolean record(String label, Answer answer) throws IOException {
        boolean served = client.request("GET", "/api/v1/books", null).status() == 200;
        List<String> present = SHOWN_HEADERS.stream().filter(name -> answer.header(name) != null).toList();
        System.out.println("RAW | " + label + " | " + answer.status() + " | " + bodyClass(answer) + " | "
                + String.join(",", present) + " | next served=" + served);
        return served;
    }

    /**
     * Returns whether a status is in the allowed set.
     *
     * @param status the status
     * @param allowed the allowed statuses
     * @return true when listed
     */
    public static boolean in(int status, int... allowed) {
        return IntStream.of(allowed).anyMatch(value -> value == status);
    }
}
