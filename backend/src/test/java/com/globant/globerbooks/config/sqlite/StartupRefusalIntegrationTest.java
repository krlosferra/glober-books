package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Reason;
import com.globant.globerbooks.support.ApiCalls.Reply;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.ConsoleRules;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.TreeSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34a-2, criteria 3, 4 and 5 on the packaged jar: every refusal ends the process with a non-zero
 * exit, a fixed reason through {@link StartupFailureAnalyzer}, no path, no book data and no stack trace
 * on the console, and the tree (names, modes, owners, link targets, hashes) is the same before and after
 * (US-009.6, US-009.7, SC-13, SC-37, SC-39, SEC-F-098, SEC-F-140, SEC-F-178, SEC-F-182, SEC-F-184, C-12).
 */
class StartupRefusalIntegrationTest {

    @TempDir
    Path temp;

    private Path root;
    private Path console;

    private Path newRoot() throws Exception {
        root = Files.createDirectory(temp.resolve("loc"), FileFacts.dirAttr("rwx------"));
        console = temp.resolve("console.log");
        return root;
    }

    private Path privateData() throws Exception {
        Path data = Files.createDirectory(newRoot().resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        return data;
    }

    private void assertRefused(String url, String fixedText) throws Exception {
        List<String> before = TreeSnapshot.snapshot(root);

        int exit = AppProcess.runToExit(url, "022", null, console);

        String text = Files.readString(console);
        System.out.println("REFUSAL exit=" + exit + " reason=\"" + fixedText + "\"\n--- console ---\n" + text
                + "--- tree before == after: " + before.equals(TreeSnapshot.snapshot(root)) + " ---");
        assertThat(exit).isNotZero();
        ConsoleRules.assertFixedAndClean(text, fixedText, temp, Path.of("library.db"));
        assertThat(TreeSnapshot.snapshot(root)).isEqualTo(before);
    }

    private static String url(Path file) {
        return "jdbc:sqlite:" + file;
    }

    @Test
    void refusesAnExistingDirectoryWithMode0755() throws Exception {
        Path data = privateData();
        Files.setPosixFilePermissions(data, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));

        assertRefused(url(data.resolve("library.db")), Reason.PERMISSIONS_TOO_WIDE.text());
    }

    @Test
    void refusesAnExistingFileWithMode0644() throws Exception {
        Path data = privateData();
        Path file = Files.createFile(data.resolve("library.db"), FileFacts.fileAttr("rw-r--r--"));
        Files.setPosixFilePermissions(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));

        assertRefused(url(file), Reason.PERMISSIONS_TOO_WIDE.text());
    }

    @Test
    void refusesASymbolicLinkAsTheDataDirectory() throws Exception {
        Path real = Files.createDirectory(newRoot().resolve("real"), FileFacts.dirAttr("rwx------"));
        Path link = Files.createSymbolicLink(root.resolve("data"), real);

        assertRefused(url(link.resolve("library.db")), Reason.SYMBOLIC_LINK.text());
    }

    @Test
    void refusesASymbolicLinkAsTheDatabaseFile() throws Exception {
        Path data = privateData();
        Path target = Files.createFile(root.resolve("elsewhere.db"), FileFacts.fileAttr("rw-------"));
        Path link = Files.createSymbolicLink(data.resolve("library.db"), target);

        assertRefused(url(link), Reason.SYMBOLIC_LINK.text());
    }

    @Test
    void refusesADatabaseFileWithTwoHardLinks() throws Exception {
        Path data = privateData();
        Path file = Files.createFile(data.resolve("library.db"), FileFacts.fileAttr("rw-------"));
        Files.createLink(root.resolve("second-name"), file);

        assertRefused(url(file), Reason.MULTIPLE_LINKS.text());
    }

    @Test
    void refusesAFileOwnedByAnotherUserWhenTheRunIsRoot() throws Exception {
        assumeTrue(0 == (Integer) Files.getAttribute(Path.of("/proc/self"), "unix:uid"),
                "needs root to chown; the unit proof of Task 34a-1 stands otherwise");
        Path data = privateData();
        Path file = Files.createFile(data.resolve("library.db"), FileFacts.fileAttr("rw-------"));
        Files.setAttribute(file, "unix:uid", 12345);

        assertRefused(url(file), Reason.FOREIGN_OWNER.text());
    }

    @Test
    void refusesADirectoryOwnedByAnotherUserWhenTheRunIsRoot() throws Exception {
        assumeTrue(0 == (Integer) Files.getAttribute(Path.of("/proc/self"), "unix:uid"),
                "needs root to chown; the unit proof of Task 34a-1 stands otherwise");
        Path data = privateData();
        Files.setAttribute(data, "unix:uid", 12345);

        assertRefused(url(data.resolve("library.db")), Reason.FOREIGN_OWNER.text());
    }

    @Test
    void refusesAFileWhereTheDirectoryShouldBe() throws Exception {
        Path blocker = Files.createFile(newRoot().resolve("data"), FileFacts.fileAttr("rw-------"));

        assertRefused(url(blocker.resolve("library.db")), Reason.WRONG_FILE_TYPE.text());
        assertThat(blocker).isRegularFile();
    }

    @Test
    void refusesAFileWhereTheDataDirectoryItselfShouldBe() throws Exception {
        newRoot();
        Path blocker = Files.createFile(root.resolve("block"), FileFacts.fileAttr("rw-------"));

        assertRefused(url(blocker.resolve("data").resolve("library.db")), Reason.PARENT_UNUSABLE.text());
    }

    @Test
    void refusesAMissingParentOfTheDataDirectoryAndCreatesNothing() throws Exception {
        newRoot();

        assertRefused(url(root.resolve("missing").resolve("data").resolve("library.db")),
                Reason.PARENT_UNUSABLE.text());
        assertThat(TreeSnapshot.names(root)).isEmpty();
    }

    @Test
    void failsCleanlyOnA300ByteFileNameAndLeavesNothingBehind() throws Exception {
        newRoot();

        assertRefused(url(root.resolve("data").resolve("x".repeat(300))), Reason.CANNOT_CREATE.text());
        assertThat(TreeSnapshot.names(root)).isEmpty();
    }

    @Test
    void refusesAUrlWithParametersThroughTheSameAnalyzer() throws Exception {
        newRoot();

        assertRefused(url(root.resolve("data").resolve("library.db")) + "?mode=ro", StartupPolicy.REASON_PARAMETERS);
        assertThat(TreeSnapshot.names(root)).isEmpty();
    }

    @Test
    void secondInstanceExitsNonZeroAndTheFirstKeepsServing() throws Exception {
        newRoot();
        Path data = root.resolve("data");
        Path firstConsole = temp.resolve("first.log");
        AppProcess first = AppProcess.start(url(data.resolve("library.db")), "022", null, firstConsole);
        try {
            Reply added = first.api().call("POST", "/api/v1/books", "{\"title\":\"One\",\"author\":\"Two\"}");
            assertThat(added.status()).isEqualTo(201);
            String listBefore = first.api().call("GET", "/api/v1/books", null).body();
            List<String> before = TreeSnapshot.snapshot(root);

            int exit = AppProcess.runToExit(url(data.resolve("library.db")), "022", null, console =
                    temp.resolve("second.log"));

            String text = Files.readString(console);
            System.out.println("SECOND INSTANCE exit=" + exit + "\n--- console ---\n" + text);
            assertThat(exit).isNotZero();
            ConsoleRules.assertFixedAndClean(text, Reason.DATABASE_UNAVAILABLE.text(), temp, Path.of("library.db"));
            assertThat(first.isAlive()).isTrue();
            assertThat(first.api().call("GET", "/api/v1/books", null).body()).isEqualTo(listBefore);
            assertThat(TreeSnapshot.snapshot(root)).isEqualTo(before);
        } finally {
            first.kill();
        }
    }
}
