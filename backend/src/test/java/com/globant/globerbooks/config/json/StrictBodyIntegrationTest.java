package com.globant.globerbooks.config.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.transport.StaticAssetCatalog;
import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real-port proof of strict body reading (spec 5.0, BR-012 step 5; Appendix D T-04, T-05, T-11,
 * T-16, T-21, T-27 to T-31, T-39). Bodies go over a raw socket so the bytes are exactly as written.
 * Expected statuses and codes are copied from the spec. Every row is recorded with its elapsed
 * time in {@code target/strict-body-real-port.txt}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StrictBodyIntegrationTest.Fixture.class)
class StrictBodyIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String MALFORMED = "MALFORMED_REQUEST";
    private static final AtomicBoolean WARMED = new AtomicBoolean();
    private static final List<String> ROWS = new ArrayList<>();
    private static final String VALID_TAIL = ",\"author\":\"b\"}";

    @TempDir
    static Path dataDir;

    @TempDir
    static Path staticDir;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void writeFixture() throws IOException {
        Files.writeString(staticDir.resolve("index.html"), "<!doctype html><title>t</title>");
    }

    /** Sends untimed requests once so class loading and the first connection are not in a timed row. */
    @BeforeEach
    void warmUp() throws IOException {
        if (WARMED.compareAndSet(false, true)) {
            RawHttpClient client = new RawHttpClient(port);
            byte[] valid = text("{\"title\":\"warm-up\",\"author\":\"b\"}");
            client.request("POST", BOOKS, valid, "Content-Type: application/json", "Content-Length: " + valid.length);
            client.request("POST", BOOKS, new byte[0], "Content-Type: application/json", "Content-Length: 0");
        }
    }

    @AfterAll
    static void recordRows() throws IOException {
        Path out = Path.of("target", "strict-body-real-port.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, ROWS);
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("strict.db"));
    }

    /** Replaces the production catalog with one built from the fixture directory. */
    @TestConfiguration
    static class Fixture {
        @Bean
        @Primary
        StaticAssetCatalog fixtureCatalog() {
            return new StaticAssetCatalog(staticDir.toUri().toString());
        }
    }

    private Answer post(String label, byte[] body) throws IOException {
        long started = System.nanoTime();
        Answer answer = new RawHttpClient(port).request("POST", BOOKS, body,
                "Content-Type: application/json", "Content-Length: " + body.length);
        long millis = (System.nanoTime() - started) / 1_000_000;
        String code = answer.body().replaceAll("(?s).*\"code\":\"([A-Z_]+)\".*", "$1");
        String shown = answer.status() == 201 ? "-" : code;
        ROWS.add(label + " | " + answer.status() + " | " + shown + " | " + millis + " ms");
        assertThat(answer.status()).isNotEqualTo(500);
        assertThat(millis).as(label + " elapsed").isLessThan(300);
        return answer;
    }

    private void expectMalformed(String label, byte[] body) throws IOException {
        Answer answer = post(label, body);
        assertThat(answer.status()).as(label).isEqualTo(400);
        assertThat(answer.body()).as(label).contains("\"code\":\"" + MALFORMED + "\"").doesNotContain("details");
    }

    private void expectMalformed(String label, String body) throws IOException {
        expectMalformed(label, body.getBytes(StandardCharsets.UTF_8));
    }

    private static String book(String extraMember) {
        return "{\"title\":\"a\",\"author\":\"b\"" + extraMember + "}";
    }

    private static byte[] bytes(int... values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int value : values) {
            out.write(value);
        }
        return out.toByteArray();
    }

    private static byte[] join(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void rejectsInvalidUtf8InsideAStringT05T28() throws IOException {
        byte[] head = text("{\"title\":\"a");
        byte[] tail = text("\"" + VALID_TAIL);
        expectMalformed("T-05 byte FF", join(head, bytes(0xFF), tail));
        expectMalformed("T-28 C0 80", join(head, bytes(0xC0, 0x80), tail));
        expectMalformed("T-28 ED A0 80", join(head, bytes(0xED, 0xA0, 0x80), tail));
        expectMalformed("T-28 F4 90 80 80", join(head, bytes(0xF4, 0x90, 0x80, 0x80), tail));
        expectMalformed("T-28 E2 82 truncated", join(head, bytes(0xE2, 0x82), tail));
    }

    @Test
    void rejectsSyntaxOutsideStrictJsonT29() throws IOException {
        expectMalformed("T-29 BOM", join(bytes(0xEF, 0xBB, 0xBF), text("{\"title\":\"a\"" + VALID_TAIL)));
        expectMalformed("T-29 comment", "/*x*/{\"title\":\"a\"" + VALID_TAIL);
        expectMalformed("T-29 trailing comma", "{\"title\":\"a\",\"author\":\"b\",}");
        expectMalformed("T-29 single quotes", "{'title':'a','author':'b'}");
        expectMalformed("T-29 unquoted names", "{title:\"a\",author:\"b\"}");
        expectMalformed("T-29 raw line feed", "{\"title\":\"a\nb\"" + VALID_TAIL);
    }

    @Test
    void rejectsUnknownDuplicateAndTrailingMembersT16T30() throws IOException {
        for (String name : List.of("id", "date_added", "date_completed", "rating", "Title")) {
            expectMalformed("T-16 member " + name, book(",\"" + name + "\":\"x\""));
        }
        expectMalformed("T-16 trailing x", "{\"title\":\"a\"" + VALID_TAIL + "x");
        expectMalformed("T-30 duplicate title", "{\"title\":\"a\",\"title\":\"b\",\"author\":\"c\"}");
        expectMalformed("T-30 duplicate after unescaping", "{\"title\":\"a\",\"\\u0074itle\":\"b\",\"author\":\"c\"}");
        expectMalformed("T-30 duplicate allow_duplicate",
                "{\"title\":\"a\",\"author\":\"b\",\"allow_duplicate\":true,\"allow_duplicate\":false}");
    }

    @Test
    void rejectsWrongTypesAndAllowDuplicateTextNumberNullT21() throws IOException {
        expectMalformed("T-21 allow_duplicate \"true\"", book(",\"allow_duplicate\":\"true\""));
        expectMalformed("T-21 allow_duplicate 1", book(",\"allow_duplicate\":1"));
        expectMalformed("T-21 allow_duplicate null", book(",\"allow_duplicate\":null"));
        expectMalformed("SEC-F-122 title number", "{\"title\":5,\"author\":\"b\"}");
        expectMalformed("SEC-F-122 author boolean", "{\"title\":\"a\",\"author\":true}");
        expectMalformed("SEC-F-122 status number", book(",\"status\":5"));
        expectMalformed("SEC-F-122 status boolean", book(",\"status\":true"));
    }

    @Test
    void rejectsUnpairedSurrogatesT04T27() throws IOException {
        expectMalformed("T-04 uD800", "{\"title\":\"\\uD800\"" + VALID_TAIL);
        expectMalformed("T-27 reversed pair", "{\"title\":\"\\uDE00\\uD83D\"" + VALID_TAIL);
        expectMalformed("T-27 uD83D then letter", "{\"title\":\"\\uD83Dx\"" + VALID_TAIL);
    }

    @Test
    void acceptsFiveHundredEscapedPairsT27() throws IOException {
        Answer answer = post("T-27 500 pairs",
                text("{\"title\":\"" + "\\uD83D\\uDE00".repeat(500) + "\"" + VALID_TAIL));
        assertThat(answer.status()).isEqualTo(201);
    }

    @Test
    void rejectsEmptyAndNonObjectBodiesT39() throws IOException {
        expectMalformed("T-39 zero bytes", new byte[0]);
        for (String body : List.of("null", "[]", "\"x\"", "5")) {
            expectMalformed("T-39 body " + body, body);
        }
    }

    @Test
    void answersLongTitleWithinTheCapAsValidationErrorT11() throws IOException {
        Answer answer = post("T-11 16,000 character title",
                text("{\"title\":\"" + "a".repeat(16_000) + "\"" + VALID_TAIL));
        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.body()).contains("\"code\":\"VALIDATION_ERROR\"");
    }

    @Test
    void answersStringFillingTheWholeCapAsValidationErrorT11() throws IOException {
        int overhead = text("{\"title\":\"\"" + VALID_TAIL).length;
        byte[] body = text("{\"title\":\"" + "a".repeat(16_384 - overhead) + "\"" + VALID_TAIL);
        assertThat(body).hasSize(16_384);
        Answer answer = post("T-11 body of exactly 16,384 bytes", body);
        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.body()).contains("\"code\":\"VALIDATION_ERROR\"");
    }

    @Test
    void answersDeepNestingAndHugeExponentsFastAsMalformedT31() throws IOException {
        expectMalformed("T-31 8,000 nested arrays",
                "{\"title\":" + "[".repeat(8_000) + "]".repeat(8_000) + VALID_TAIL);
        expectMalformed("T-31 2,000 nested objects",
                book(",\"x\":" + "{\"x\":".repeat(2_000) + "1" + "}".repeat(2_000)));
        expectMalformed("T-31 exponent 1e999999999", book(",\"status\":1e999999999"));
        expectMalformed("T-31 exponent 1e-999999999", book(",\"status\":1e-999999999"));
    }

    @Test
    void keepsAbsentStatusAsUnreadAndNullStatusAsInvalidStatus() throws IOException {
        Answer absent = post("absent status", text("{\"title\":\"absent status\"" + VALID_TAIL));
        assertThat(absent.status()).isEqualTo(201);
        assertThat(absent.body()).contains("\"status\":\"unread\"");
        Answer explicit = post("null status", text(book(",\"status\":null")));
        assertThat(explicit.status()).isEqualTo(400);
        assertThat(explicit.body()).contains("\"code\":\"INVALID_STATUS\"");
    }

    @Test
    void neverEchoesBodyContentOrParserMessages() throws IOException {
        Answer answer = post("no echo", text(book(",\"SECRETMARK\":\"SECRETVALUE\"")));
        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.body()).doesNotContain("SECRETMARK", "SECRETVALUE", "Unrecognized", "jackson", "Unexpected");
        Answer syntax = post("no echo syntax", text("{\"title\":SECRETVALUE}"));
        assertThat(syntax.body()).doesNotContain("SECRETVALUE", "Unrecognized", "Unexpected", "column");
        assertThat(syntax.body()).contains("\"error\":\"The request is malformed.\"");
    }
}
