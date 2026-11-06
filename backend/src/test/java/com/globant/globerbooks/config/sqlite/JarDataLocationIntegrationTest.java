package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FileFacts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34a-2, criteria 1 and 2 on the packaged jar: started on an empty location the application creates
 * the directory (0700), {@code tmp} (0700) and {@code library.db} (0600) with the given umask 022 or 000, and
 * every companion file seen during and after writes is no wider than 0600 (US-009.6, SC-13, SEC-F-181). The
 * pragmas cannot be read from outside the process without a test-only switch (A-09); they are read back in
 * process in {@code SqliteWiringIntegrationTest}.
 */
class JarDataLocationIntegrationTest {

    private static final int ADDS = 60;

    @TempDir
    Path temp;

    private void startUnderUmaskAndAdd(String umask) throws Exception {
        Path data = temp.resolve("fresh").resolve("data");
        Files.createDirectory(data.getParent(), FileFacts.dirAttr("rwx------"));
        Map<String, String> seen = new ConcurrentHashMap<>();
        AtomicBoolean running = new AtomicBoolean(true);
        AppProcess app = AppProcess.start("jdbc:sqlite:" + data.resolve("library.db"), umask, null,
                temp.resolve("console.log"));
        Thread watcher = new Thread(() -> watch(data, seen, running));
        try {
            assertThat(FileFacts.mode(data)).isEqualTo("rwx------");
            assertThat(FileFacts.mode(data.resolve("tmp"))).isEqualTo("rwx------");
            assertThat(FileFacts.mode(data.resolve("library.db"))).isEqualTo("rw-------");
            try (Stream<Path> natives = Files.list(data.resolve("tmp"))) {
                assertThat(natives.toList()).as("native library files").isNotEmpty()
                        .allMatch(FileFacts::isNotWiderThan0600);
            }
            watcher.start();
            for (int index = 0; index < ADDS; index++) {
                Reply added = app.api().call("POST", "/api/v1/books",
                        "{\"title\":\"Title " + index + "\",\"author\":\"Author\"}");
                assertThat(added.status()).isEqualTo(201);
            }
        } finally {
            running.set(false);
            watcher.join();
            assertThat(app.stop()).isIn(0, 143);
        }
        System.out.println("UMASK " + umask + " files seen by the watcher during " + ADDS + " adds: " + seen);
        assertThat(seen.values()).allMatch("rw-------"::equals);
        assertThat(FileFacts.mode(data)).isEqualTo("rwx------");
        assertThat(FileFacts.mode(data.resolve("tmp"))).isEqualTo("rwx------");
        try (Stream<Path> entries = Files.list(data)) {
            entries.filter(Files::isRegularFile).forEach(path -> assertThat(FileFacts.isNotWiderThan0600(path))
                    .as(path.getFileName() + " " + FileFacts.mode(path)).isTrue());
        }
    }

    private static void watch(Path data, Map<String, String> seen, AtomicBoolean running) {
        while (running.get()) {
            try (Stream<Path> entries = Files.list(data)) {
                entries.filter(path -> Files.isRegularFile(path)).forEach(path -> record(path, seen));
            } catch (IOException gone) {
                continue;
            }
        }
    }

    private static void record(Path path, Map<String, String> seen) {
        try {
            seen.put(path.getFileName().toString(), FileFacts.mode(path));
        } catch (IllegalStateException vanished) {
            return;
        }
    }

    @Test
    void createsThePrivateLocationUnderUmask022() throws Exception {
        startUnderUmaskAndAdd("022");
    }

    @Test
    void createsThePrivateLocationUnderUmask000() throws Exception {
        startUnderUmaskAndAdd("000");
    }

    @Test
    void theShippedDefaultRelativeLocationWorksWhenStartedFromTheWorkingDirectory() throws Exception {
        AppProcess app = AppProcess.start(null, "022", temp, temp.resolve("console-default.log"));
        try {
            Path data = temp.resolve("data");
            assertThat(FileFacts.mode(data)).isEqualTo("rwx------");
            assertThat(FileFacts.mode(data.resolve("tmp"))).isEqualTo("rwx------");
            assertThat(FileFacts.mode(data.resolve("globerbooks.db"))).isEqualTo("rw-------");
            assertThat(app.api().call("POST", "/api/v1/books", "{\"title\":\"A\",\"author\":\"B\"}").status())
                    .isEqualTo(201);
            assertThat(app.api().call("GET", "/api/v1/books", null).status()).isEqualTo(200);
        } finally {
            app.stop();
        }
    }
}
