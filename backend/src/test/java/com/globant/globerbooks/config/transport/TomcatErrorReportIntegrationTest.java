package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Raw-socket proof that a container-level error page names no product, version or report (SEC-F-066). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TomcatErrorReportIntegrationTest {

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("t.db"));
    }

    private String raw(String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
            socket.getOutputStream().flush();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            socket.getInputStream().transferTo(out);
            return out.toString(StandardCharsets.ISO_8859_1);
        }
    }

    @Test
    void invalidRequestLineShowsNoProductVersionOrReport() throws Exception {
        String response = raw("GET / HTTP/1.1 junk junk\r\nHost: localhost:" + port + "\r\n\r\n");
        assertThat(response).startsWith("HTTP/1.1 400");
        assertThat(response).doesNotContainIgnoringCase("tomcat").doesNotContainIgnoringCase("apache")
                .doesNotContainIgnoringCase("Server:").doesNotContainIgnoringCase("Exception")
                .doesNotContainIgnoringCase("Type Exception Report").doesNotContainIgnoringCase("Description");
    }
}
