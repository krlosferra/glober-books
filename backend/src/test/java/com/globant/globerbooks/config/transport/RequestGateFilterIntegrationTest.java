package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.globant.globerbooks.service.BookService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Real-port proof of the gate and the advice over a raw socket; prints the evidence table. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestGateFilterIntegrationTest {

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("gate.db"));
    }

    @MockitoBean
    private BookService service;

    private String exchange(String head, byte[] body, boolean readBodyFirst) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(head.getBytes(StandardCharsets.ISO_8859_1));
            if (body != null) {
                try {
                    out.write(body);
                } catch (IOException closed) {
                    // the server may close after an early rejection
                }
            }
            out.flush();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                socket.getInputStream().transferTo(buffer);
            } catch (IOException reset) {
                // connection reset after the response
            }
            return buffer.toString(StandardCharsets.ISO_8859_1);
        }
    }

    private String get(String path, String... headers) throws IOException {
        return exchange(request("GET", path, headers) + "Connection: close\r\n\r\n", null, false);
    }

    private String request(String method, String path, String... headers) {
        StringBuilder text = new StringBuilder(method + " " + path + " HTTP/1.1\r\n");
        boolean hasHost = false;
        for (String header : headers) {
            hasHost |= header.toLowerCase(Locale.ROOT).startsWith("host:");
            text.append(header).append("\r\n");
        }
        if (!hasHost) {
            text.append("Host: localhost:").append(port).append("\r\n");
        }
        return text.toString();
    }

    private static void row(String label, String response) {
        String compact = response.replace("\r\n\r\n", " || ").replace("\r\n", " | ");
        String shown = compact.replaceAll("\\| (Cross-Origin|Referrer|Cache-Control|X-Content|X-Frame)[^|]*", "")
                .replaceAll("\\| (Content-Security|Permissions)[^|]*", "");
        System.out.println("EVIDENCE " + label + " => " + shown.trim());
    }

    private static void assertStatus(String response, int status, String code) {
        assertThat(response).startsWith("HTTP/1.1 " + status);
        if (code != null) {
            assertThat(response).contains("\"code\":\"" + code + "\"");
        }
    }

    @Test
    void forgedAndAllowedHosts() throws IOException {
        for (String host : new String[] {"evil.example", "localhost", "localhost:1", "[::1]:" + port}) {
            String response = get("/api/v1/books", "Host: " + host);
            row("host " + host, response);
            assertStatus(response, 400, "INVALID_HOST");
        }
        for (String host : new String[] {"LOCALHOST:" + port, "127.0.0.1:" + port}) {
            String response = get("/nothing", "Host: " + host);
            row("host " + host, response);
            assertStatus(response, 404, "NOT_FOUND");
        }
    }

    @Test
    void originFetchSiteAndContentType() throws IOException {
        String origin = get("/nothing", "Origin: http://evil.example");
        row("origin evil", origin);
        assertStatus(origin, 403, "FORBIDDEN");
        String site = get("/nothing", "Sec-Fetch-Site: cross-site");
        row("sec-fetch-site cross-site", site);
        assertStatus(site, 403, "FORBIDDEN");
        for (String method : new String[] {"POST", "PUT", "DELETE"}) {
            String none = exchange(request(method, "/nothing", "Content-Length: 0") + "Connection: close\r\n\r\n",
                    null, false);
            row(method + " no content-type", none);
            assertStatus(none, 415, "UNSUPPORTED_MEDIA_TYPE");
            String wrong = exchange(request(method, "/nothing", "Content-Type: text/plain", "Content-Length: 0")
                    + "Connection: close\r\n\r\n", null, false);
            row(method + " text/plain", wrong);
            assertStatus(wrong, 415, "UNSUPPORTED_MEDIA_TYPE");
        }
    }

    @Test
    void declaredOverLimitBodyIsRejectedWithoutContinue() throws IOException {
        String head = request("POST", "/api/v1/books", "Content-Type: application/json", "Content-Length: 16385",
                "Expect: 100-continue") + "\r\n";
        String response = exchange(head, null, false);
        row("declared 16385 + Expect 100-continue (no body sent)", response);
        assertStatus(response, 413, "PAYLOAD_TOO_LARGE");
        assertThat(response).doesNotContain("100 Continue").containsIgnoringCase("Connection: close");
    }

    @Test
    void chunkedBodyCrossingTheLimitIsRejectedAndTheLimitItselfPasses() throws IOException {
        String response = exchange(chunkedHead(), chunks(16_385), false);
        row("chunked 16385", response);
        assertStatus(response, 413, "PAYLOAD_TOO_LARGE");
        String accepted = exchange(chunkedHead(), chunks(16_384), false);
        row("chunked 16384", accepted);
        assertStatus(accepted, 400, "MALFORMED_REQUEST");
    }

    private String chunkedHead() {
        return request("POST", "/api/v1/books", "Content-Type: application/json", "Transfer-Encoding: chunked",
                "Connection: close") + "\r\n";
    }

    private byte[] chunks(int total) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int left = total;
        while (left > 0) {
            int size = Math.min(left, 4096);
            out.writeBytes((Integer.toHexString(size) + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.writeBytes(" ".repeat(size).getBytes(StandardCharsets.ISO_8859_1));
            out.writeBytes("\r\n".getBytes(StandardCharsets.ISO_8859_1));
            left -= size;
        }
        out.writeBytes("0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    @Test
    void unexpectedExceptionAndXmlAcceptAnswerJsonWithHeaders() throws IOException {
        when(service.list(null)).thenThrow(new IllegalStateException("secret user text"));
        String boom = get("/api/v1/books", "Accept: application/json");
        row("forced exception, Accept application/json", boom);
        assertStatus(boom, 500, "INTERNAL_ERROR");
        assertThat(boom).containsIgnoringCase("Content-Type: application/json;charset=UTF-8")
                .doesNotContain("secret").doesNotContainIgnoringCase("Server:")
                .doesNotContainIgnoringCase("Set-Cookie").contains("X-Content-Type-Options: nosniff")
                .contains("Cache-Control: no-store").contains("Content-Security-Policy: default-src 'self'");
        String missing = get("/nothing");
        row("unknown path (sendError path of the resource handler)", missing);
        assertStatus(missing, 404, "NOT_FOUND");
        assertThat(missing).contains("X-Frame-Options: DENY");
    }
}
