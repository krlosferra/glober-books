package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FileFacts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * The packaged application as a separate process: acknowledged writes survive a normal stop and a forced
 * kill (spec US-009.1, US-009.2, C-13, M-3; security SC-13). The kill is SIGKILL, sent right after the
 * answer of the write, 5 times for each of add, status change, rating change and delete (C-13 asks for 50
 * rounds in all; this task fixes 5 per operation). Every title and author carries the marker
 * {@link #MARKER}; the console of every process must not contain it.
 */
class DurabilityIntegrationTest {

    private static final String BOOKS = "/api/v1/books";
    private static final String MARKER = "ZQXDATA";
    private static final int KILLS = 5;
    private static final String[] STATUS_CYCLE = {"reading", "completed", "unread", "completed", "reading"};

    @TempDir
    Path temp;

    private Path dataDir;
    private final List<AppProcess> started = new ArrayList<>();
    private final List<Path> consoles = new ArrayList<>();
    private final JsonMapper json = JsonMapper.builder().build();

    @AfterEach
    void stopEverythingAndCheckTheConsoles() throws Exception {
        for (AppProcess app : started) {
            app.kill();
        }
        for (AppProcess app : started) {
            assertThat(app.isAlive()).isFalse();
            String text = app.console();
            assertThat(text).as("console is captured").contains("Started GloberBooksApplication");
            assertThat(text).as("the console holds no book data").doesNotContain(MARKER);
        }
    }

    private AppProcess launch() throws Exception {
        dataDir = temp.resolve("data");
        if (!Files.exists(dataDir)) {
            Files.createDirectory(dataDir, FileFacts.dirAttr("rwx------"));
        }
        Path database = dataDir.resolve("library.db");
        if (!Files.exists(database)) {
            Files.createFile(database, FileFacts.fileAttr("rw-------"));
        }
        Path console = Files.createTempFile(temp, "console", ".log");
        consoles.add(console);
        AppProcess app = AppProcess.start(dataDir, console);
        started.add(app);
        return app;
    }

    /**
     * The directory is not wider than 0700 and every file in it (database, journal, wal, shm) not wider than
     * 0600, all owned by the test user: the application never widens them (SC-13).
     * NOTE: the CREATION of the directory and file with these modes by the packaged application is not proved
     * here; the test pre-creates them like the E2E launcher. Creation modes are proved in Task 34a (SC-13).
     */
    private void assertPrivate() throws IOException {
        Set<PosixFilePermission> directory = PosixFilePermissions.fromString("rwx------");
        Set<PosixFilePermission> file = PosixFilePermissions.fromString("rw-------");
        try (Stream<Path> walk = Files.walk(dataDir)) {
            for (Path entry : walk.toList()) {
                assertThat(Files.getOwner(entry)).as(entry + " owner").isEqualTo(Files.getOwner(temp));
                Set<PosixFilePermission> mode = Files.getPosixFilePermissions(entry);
                assertThat(directory.containsAll(mode) && (Files.isDirectory(entry) || file.containsAll(mode)))
                        .as(entry + " has mode " + PosixFilePermissions.toString(mode)).isTrue();
            }
        }
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dataDir))).isEqualTo("rwx------");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dataDir.resolve("library.db"))))
                .isEqualTo("rw-------");
    }

    private Reply send(AppProcess app, String method, String path, String body) throws Exception {
        Reply reply = app.api().call(method, path, body);
        assertThat(reply.status()).as(method + " " + path + " " + reply.body()).isBetween(200, 299);
        return reply;
    }

    private String add(AppProcess app, String label, String status) throws Exception {
        String body = "{\"title\":\"" + MARKER + " title " + label + "\",\"author\":\"" + MARKER
                + " author\",\"status\":\"" + status + "\"}";
        return json.readTree(send(app, "POST", BOOKS, body).body()).path("id").asString();
    }

    private String list(AppProcess app) throws Exception {
        return send(app, "GET", BOOKS, null).body();
    }

    private AppProcess normalRestart(AppProcess app) throws Exception {
        assertThat(app.stop()).as("graceful stop").isIn(0, 143);
        assertPrivate();
        AppProcess next = launch();
        assertPrivate();
        return next;
    }

    @Test
    void normalStopAndStartShowTheSameDataAfterAddStatusRatingAndDeleteUs0091() throws Exception {
        AppProcess app = launch();
        assertPrivate();
        String keep = add(app, "keep", "unread");
        String gone = add(app, "gone", "reading");
        String after = list(app);
        app = normalRestart(app);
        assertThat(list(app)).as("after add").isEqualTo(after);

        send(app, "PUT", BOOKS + "/" + keep + "/status", "{\"status\":\"completed\"}");
        after = list(app);
        app = normalRestart(app);
        assertThat(list(app)).as("after status change").isEqualTo(after);

        send(app, "PUT", BOOKS + "/" + keep + "/rating", "{\"rating\":4}");
        after = list(app);
        app = normalRestart(app);
        assertThat(list(app)).as("after rating change").isEqualTo(after);
        assertThat(after).contains("\"rating\":4");

        send(app, "DELETE", BOOKS + "/" + gone, null);
        after = list(app);
        app = normalRestart(app);
        assertThat(list(app)).as("after delete").isEqualTo(after).doesNotContain(gone);
    }

    private AppProcess killAndRestart(AppProcess app, String operation, int round) throws Exception {
        app.kill();
        assertThat(app.isAlive()).isFalse();
        assertPrivate();
        AppProcess next = launch();
        assertPrivate();
        System.out.println("EVIDENCE | kill after " + operation + " round " + round + " | restart opened, "
                + next.startupMillis() + " ms");
        return next;
    }

    @Test
    void forcedKillRightAfterEachAcknowledgedAddKeepsTheBookC13() throws Exception {
        AppProcess app = launch();
        for (int round = 1; round <= KILLS; round++) {
            Reply added = send(app, "POST", BOOKS, "{\"title\":\"" + MARKER + " add " + round + "\",\"author\":\""
                    + MARKER + " author\"}");
            String id = json.readTree(added.body()).path("id").asString();

            app = killAndRestart(app, "add", round);

            assertThat(send(app, "GET", BOOKS + "/" + id, null).body()).isEqualTo(added.body());
        }
        assertThat(json.readTree(list(app)).path("total").asInt()).isEqualTo(KILLS);
    }

    @Test
    void forcedKillRightAfterEachAcknowledgedStatusChangeKeepsTheStatusC13() throws Exception {
        AppProcess app = launch();
        String id = add(app, "status", "unread");
        for (int round = 1; round <= KILLS; round++) {
            Reply changed = send(app, "PUT", BOOKS + "/" + id + "/status",
                    "{\"status\":\"" + STATUS_CYCLE[round - 1] + "\"}");

            app = killAndRestart(app, "status", round);

            assertThat(send(app, "GET", BOOKS + "/" + id, null).body()).isEqualTo(changed.body());
            assertThat(json.readTree(changed.body()).path("status").asString()).isEqualTo(STATUS_CYCLE[round - 1]);
        }
    }

    @Test
    void forcedKillRightAfterEachAcknowledgedRatingChangeKeepsTheRatingC13() throws Exception {
        AppProcess app = launch();
        String id = add(app, "rating", "completed");
        for (int round = 1; round <= KILLS; round++) {
            Reply rated = send(app, "PUT", BOOKS + "/" + id + "/rating", "{\"rating\":" + round + "}");

            app = killAndRestart(app, "rating", round);

            assertThat(send(app, "GET", BOOKS + "/" + id, null).body()).isEqualTo(rated.body());
            assertThat(json.readTree(rated.body()).path("rating").asInt()).isEqualTo(round);
        }
    }

    @Test
    void forcedKillRightAfterEachAcknowledgedDeleteKeepsTheBookDeletedC13() throws Exception {
        AppProcess app = launch();
        String survivor = add(app, "survivor", "unread");
        for (int round = 1; round <= KILLS; round++) {
            String id = add(app, "doomed " + round, "unread");
            send(app, "DELETE", BOOKS + "/" + id, null);

            app = killAndRestart(app, "delete", round);

            Reply gone = app.api().call("GET", BOOKS + "/" + id, null);
            assertThat(gone.status()).isEqualTo(404);
            assertThat(gone.code()).isEqualTo("BOOK_NOT_FOUND");
        }
        assertThat(json.readTree(list(app)).path("total").asInt()).isEqualTo(1);
        assertThat(send(app, "GET", BOOKS + "/" + survivor, null).status()).isEqualTo(200);
    }
}
