package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.sqlite.StartupPolicy;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FreshApp;
import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Header grammar rows on the real port against the packaged jar (Task 49+50, criterion 5 first part and the
 * application rows of E-24, E-24b and E-25; spec Appendix D E-34 rows 2 to 7, E-35, E-10; SEC-F-106). Expected
 * statuses are copied from the spec rows.
 */
class GrammarCornersIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String JSON = "Content-Type: application/json";

    @TempDir
    static Path temp;

    private static AppProcess app;
    private static RawHttpClient raw;
    private static int port;
    private static int counter;

    @BeforeAll
    static void start() throws Exception {
        app = FreshApp.start(temp);
        port = app.api().port();
        raw = new RawHttpClient(port);
    }

    @AfterAll
    static void stop() throws Exception {
        FreshApp.stopAndCheck(app);
    }

    private Answer post(String... contentTypeLines) throws IOException {
        counter++;
        byte[] body = ("{\"title\":\"Grammar title " + counter + "\",\"author\":\"Grammar author\"}")
                .getBytes(StandardCharsets.UTF_8);
        String[] headers = new String[contentTypeLines.length + 1];
        System.arraycopy(contentTypeLines, 0, headers, 0, contentTypeLines.length);
        headers[contentTypeLines.length] = "Content-Length: " + body.length;
        return raw.request("POST", BOOKS, body, headers);
    }

    @Test
    void e34ContentTypeRows2To7GiveTheStatedStatuses() throws IOException {
        record Row(String label, int status, String... lines) {
        }
        List<Row> rows = List.of(
                new Row("application/json ; charset=utf-8", 201, "Content-Type: application/json ; charset=utf-8"),
                new Row("charset quoted", 415, "Content-Type: application/json; charset=\"utf-8\""),
                new Row("extra parameter foo", 415, "Content-Type: application/json; charset=utf-8; foo=bar"),
                new Row("boundary", 415, "Content-Type: application/json; boundary=x"),
                new Row("two media types", 415, "Content-Type: application/json, text/plain"),
                new Row("two headers", 415, JSON, JSON));
        for (Row row : rows) {
            Answer answer = post(row.lines());
            System.out.println("GRAMMAR E-34 | " + row.label() + " | " + answer.status());
            assertThat(answer.status()).as(row.label()).isEqualTo(row.status());
        }
        assertThat(post("Content-Type: Application/JSON").status()).as("row 1").isEqualTo(201);
    }

    @Test
    void e35AcceptRowsGiveTheStatedStatuses() throws IOException {
        record Row(String accept, int status) {
        }
        List<Row> rows = List.of(new Row("application/json;q=0", 406),
                new Row("text/html,application/json;q=0.5", 200), new Row("text/xml, */*;q=0.1", 200),
                new Row("*/*;q=0", 406), new Row(";;;", 406), new Row("text/html, application/json;q=0", 406),
                new Row("", 200), new Row("application/json;q=1.5", 406), new Row("application/json;Q=0", 406),
                new Row("text/html,,application/json", 200), new Row("application/json;q=0, */*", 200));
        for (Row row : rows) {
            Answer answer = raw.request("GET", BOOKS, null, "Accept: " + row.accept());
            System.out.println("GRAMMAR E-35 | [" + row.accept() + "] | " + answer.status());
            assertThat(answer.status()).as("Accept [" + row.accept() + "]").isEqualTo(row.status());
        }
    }

    @Test
    void e24bAndE25ApplicationRowsGiveTheStatedEnvelopes() throws IOException {
        String id = UUID.randomUUID().toString();
        String[][] rows = {{BOOKS + "/.%2E", "404", "NOT_FOUND"}, {BOOKS + "/" + id + "%3b", "404", "NOT_FOUND"},
            {BOOKS + "?status=%zz", "400", "MALFORMED_REQUEST"}, {BOOKS + "?status=%FF", "400", "MALFORMED_REQUEST"}};
        for (String[] row : rows) {
            Answer answer = raw.request("GET", row[0], null);
            assertThat(answer.status()).as(row[0]).isEqualTo(Integer.parseInt(row[1]));
            assertThat(answer.body()).as(row[0]).contains("\"code\":\"" + row[2] + "\"");
        }
    }

    @Test
    void sec106HostWithAnOutOfRangeOctetIsRefusedAsInvalidHost() throws IOException {
        Answer answer = raw.request("GET", BOOKS, null, "Host: 127.0.0.256:" + port);
        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.body()).contains("\"code\":\"INVALID_HOST\"");
    }

    @Test
    void sec106BindAddressWithAnOutOfRangeOctetIsRefusedByTheStartPolicy() {
        for (String address : List.of("127.0.0.256", "127.0.0.300", "999.0.0.1")) {
            assertThatThrownBy(() -> StartupPolicy.validate(address, "jdbc:sqlite:./data/globerbooks.db"))
                    .as(address).isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_ADDRESS);
        }
    }
}
