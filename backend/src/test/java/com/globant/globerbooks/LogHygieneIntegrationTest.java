package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FreshApp;
import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Log neutralisation on the real console of the packaged jar (Task 49+50, criterion 7; spec Appendix D T-33;
 * SC-37). Hostile ids, query values and titles carry the planted markers and the characters CR, LF, U+2028,
 * U+2029, U+0085 and ESC, each in a failing and in a succeeding request. The test shows first that every
 * marker was in the request, so the absence in the log is not vacuous; then the captured console is judged:
 * one line per event (no raw line separator inside an event, no ESC) and no title text.
 */
class LogHygieneIntegrationTest {

    private static final String ID_MARKER = "ZQXLOGID";
    private static final String QUERY_MARKER = "ZQXLOGQUERY";
    private static final String TITLE_MARKER = "ZQXLOGTITLE";
    private static final String BOOKS = "/api/v1/books";
    private static final String ENCODED_HOSTILE = "%0d%0a%e2%80%a8%e2%80%a9%c2%85%1b[2J";
    private static final String ENCODED_QUERY = "%0d%0a%e2%80%a8%e2%80%a9%c2%85%1b%5b2J";
    private static final String RAW_HOSTILE = "\r\n  \u0085\u001b[2J";

    @TempDir
    static Path temp;

    private static AppProcess app;
    private static RawHttpClient raw;

    @BeforeAll
    static void start() throws Exception {
        app = FreshApp.start(temp);
        raw = new RawHttpClient(app.api().port());
    }

    @AfterAll
    static void stop() throws Exception {
        FreshApp.stopAndCheck(app);
    }

    private Answer get(String path) throws Exception {
        assertThat(path).as("the request carries the planted text").containsAnyOf(ID_MARKER, QUERY_MARKER, "?status");
        return raw.request("GET", path, null);
    }

    @Test
    void hostileIdsQueryValuesAndTitlesLeaveOneCleanLogLinePerEventAndNoTitleText() throws Exception {
        Answer badId = get(BOOKS + "/" + ID_MARKER + ENCODED_HOSTILE);
        Answer badStatus = get(BOOKS + "?status=" + QUERY_MARKER + ENCODED_QUERY);
        Answer goodList = get(BOOKS + "?status=reading");
        assertThat(badId.status()).as("id row, as in T-14 (or a pre-application 400)").isEqualTo(400);
        assertThat(badStatus.status()).as("status row, as in T-20").isEqualTo(400);
        assertThat(badStatus.body()).contains("\"code\":\"INVALID_STATUS\"");
        assertThat(goodList.status()).isEqualTo(200);

        String failingTitle = TITLE_MARKER + RAW_HOSTILE;
        assertThat(failingTitle).as("the title carries the planted text").contains(TITLE_MARKER).contains(" ");
        Reply failing = app.api().call("POST", BOOKS, "{\"title\":\"" + TITLE_MARKER
                + "\\r\\n\\u2028\\u2029\\u0085\\u001b[2J\",\"author\":\"Log author\"}");
        Reply succeeding = app.api().call("POST", BOOKS,
                "{\"title\":\"" + TITLE_MARKER + " accepted\",\"author\":\"Log author\"}");
        assertThat(failing.status()).as("hostile title").isEqualTo(400);
        assertThat(failing.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(succeeding.status()).as("plain title, as in T-12").isEqualTo(201);

        String console = app.console();
        System.out.println("LOG lines=" + console.split("\n", -1).length + " bytes="
                + console.getBytes(StandardCharsets.UTF_8).length + " lines with an id or query marker="
                + console.lines().filter(line -> line.contains(ID_MARKER) || line.contains(QUERY_MARKER)).count()
                + " WARN or ERROR lines=" + console.lines().filter(line -> line.contains(" WARN ")
                        || line.contains(" ERROR ")).count());
        assertThat(console).as("the console was captured").contains("Started GloberBooksApplication");
        for (String forbidden : List.of("\r", " ", " ", "\u0085", "\u001b")) {
            assertThat(console).as("raw character U+" + Integer.toHexString(forbidden.charAt(0)))
                    .doesNotContain(forbidden);
        }
        assertThat(console).as("no title text").doesNotContain(TITLE_MARKER);
        for (String line : console.split("\n")) {
            if (line.contains(ID_MARKER) || line.contains(QUERY_MARKER)) {
                assertThat(line).as("an event is one line").doesNotContain(" ");
            }
        }
    }
}
