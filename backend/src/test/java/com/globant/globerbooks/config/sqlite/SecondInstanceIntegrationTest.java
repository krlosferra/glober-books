package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Reason;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.ConsoleRules;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34b, criterion 5 and C-12 through the validator path (US-009.7, DEC-032): a second instance started on
 * a stored library that a running instance holds runs the header check and the claim, must not report the
 * library as damaged or invalid, exits non-zero with the fixed "in use" reason, and the first instance keeps
 * serving with the full list body, and the hash of it, unchanged. The 34a-2 test of the same name in
 * {@code StartupRefusalIntegrationTest} stays; this one adds a populated library and a first instance that is
 * writing while the second instances start.
 */
class SecondInstanceIntegrationTest {

    private static final String LIST = "/api/v1/books";

    @TempDir
    Path temp;

    private Path newLibrary(int books) throws Exception {
        Path root = Files.createDirectory(temp.resolve("loc"), FileFacts.dirAttr("rwx------"));
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        return StoredLibraryFixtures.library(data.resolve("library.db"), books);
    }

    private static String hashOf(String body) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(body.getBytes(StandardCharsets.UTF_8)));
    }

    private int runSecond(Path file, String name) throws Exception {
        Path console = temp.resolve(name);
        int exit = AppProcess.runToExit("jdbc:sqlite:" + file, "022", null, console);
        String text = Files.readString(console);
        System.out.println("SECOND INSTANCE (" + name + ") exit=" + exit + " reason=\""
                + Reason.DATABASE_UNAVAILABLE.text() + "\"\n--- console ---\n" + text);
        ConsoleRules.assertFixedAndClean(text, Reason.DATABASE_UNAVAILABLE.text() + ".", temp, Path.of("library.db"));
        Arrays.stream(StoredLibraryValidator.Reason.values())
                .forEach(other -> assertThat(text).doesNotContain(other.text()));
        return exit;
    }

    @Test
    void secondInstanceOnAPopulatedLibraryExitsInUseAndTheFirstKeepsServingUnchanged() throws Exception {
        Path file = newLibrary(25);
        AppProcess first = AppProcess.start("jdbc:sqlite:" + file, "022", null, temp.resolve("first.log"));
        try {
            String listBefore = first.api().call("GET", LIST, null).body();
            String hashBefore = hashOf(listBefore);

            int exit = runSecond(file, "second.log");

            String listAfter = first.api().call("GET", LIST, null).body();
            System.out.println("FIRST INSTANCE alive=" + first.isAlive() + " list_sha_before=" + hashBefore
                    + " list_sha_after=" + hashOf(listAfter));
            assertThat(exit).isNotZero();
            assertThat(first.isAlive()).isTrue();
            assertThat(listAfter).isEqualTo(listBefore);
            assertThat(hashOf(listAfter)).isEqualTo(hashBefore);
        } finally {
            first.kill();
        }
    }

    @Test
    void secondInstancesStartedWhileTheFirstIsWritingNeverReportDamageAndLoseNoAcknowledgedAdd() throws Exception {
        Path file = newLibrary(5);
        AppProcess first = AppProcess.start("jdbc:sqlite:" + file, "022", null, temp.resolve("first.log"));
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger acknowledged = new AtomicInteger();
        Thread writer = new Thread(() -> addUntilStopped(first, running, acknowledged));
        try {
            writer.start();
            assertThat(runSecond(file, "second-1.log")).isNotZero();
            assertThat(runSecond(file, "second-2.log")).isNotZero();
        } finally {
            running.set(false);
            writer.join();
        }
        try {
            String list = first.api().call("GET", LIST, null).body();
            assertThat(list.split("\"id\"", -1).length - 1).isEqualTo(5 + acknowledged.get());
            assertThat(first.isAlive()).isTrue();
        } finally {
            first.kill();
        }
    }

    private static void addUntilStopped(AppProcess app, AtomicBoolean running, AtomicInteger acknowledged) {
        try {
            for (int index = 0; running.get(); index++) {
                Reply added = app.api().call("POST", LIST, "{\"title\":\"Live " + index + "\",\"author\":\"Writer\"}");
                if (added.status() == 201) {
                    acknowledged.incrementAndGet();
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
