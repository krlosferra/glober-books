package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.sqlite.StoredLibraryValidator.Reason;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.ConsoleRules;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.HotJournalWriter;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import com.globant.globerbooks.support.TreeSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34b, criterion 3 on the packaged jar (CR-9 as reworded, SEC-F-074, R-20, US-009.5): a journal left by
 * a writer that was killed with SIGKILL in the middle of a transaction that grew the file is SQLite's own
 * genuine hot journal ({@code HotJournalWriter}). (i) With a valid file it is rolled back and the library is
 * accepted; (ii) with an invalid file the start is refused with a fixed reason and the main file after the
 * refusal has the SHA-256 it has after a reference rollback on a copy (no write after the refusal); (iii) a
 * planted garbage journal beside a valid and beside an invalid file changes nothing in the main file.
 */
class HotJournalIntegrationTest {

    private static final int BOOKS = 50;

    @TempDir
    Path temp;

    private Path root;
    private Path file;

    private void newLibrary(boolean withExtraTable) throws Exception {
        root = Files.createDirectory(temp.resolve("loc"), FileFacts.dirAttr("rwx------"));
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        file = StoredLibraryFixtures.library(data.resolve("library.db"), BOOKS);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET title = '" + StoredLibraryFixtures.MARKER
                + " title' WHERE id = '" + StoredLibraryFixtures.id(1) + "'");
        if (withExtraTable) {
            StoredLibraryFixtures.run(file, false, "CREATE TABLE extra (name TEXT)");
        }
    }

    private String leaveJournalAndReferenceHash() throws Exception {
        long committedSize = Files.size(file);
        HotJournalWriter.leaveHotJournal(file);
        Path scratch = Files.createDirectory(temp.resolve("reference"));
        String reference = HotJournalWriter.hashAfterReferenceRollback(file, scratch);
        System.out.println("HOT JOURNAL produced by SIGKILL of a writer JVM mid-transaction: committed size="
                + committedSize + " size after kill=" + Files.size(file) + " journal bytes="
                + Files.size(HotJournalWriter.journalOf(file)) + " sha_before_start=" + FileFacts.sha256(file)
                + " sha_after_reference_rollback=" + reference);
        assertThat(Files.size(file)).as("the file grew").isGreaterThan(committedSize);
        return reference;
    }

    private static void plantGarbageJournal(Path database) throws Exception {
        byte[] garbage = new byte[700];
        for (int index = 0; index < garbage.length; index++) {
            garbage[index] = (byte) (0x41 + index % 23);
        }
        Files.write(HotJournalWriter.journalOf(database), garbage);
    }

    private int runRefused(Path console) throws Exception {
        return AppProcess.runToExit("jdbc:sqlite:" + file, "022", null, console);
    }

    private static int countBooks(String body) {
        return body.split("\"id\"", -1).length - 1;
    }

    @Test
    void validFileWithAGenuineHotJournalIsRolledBackAndAccepted() throws Exception {
        newLibrary(false);
        String reference = leaveJournalAndReferenceHash();
        AppProcess app = AppProcess.start("jdbc:sqlite:" + file, "022", null, temp.resolve("console.log"));
        try {
            String list = app.api().call("GET", "/api/v1/books", null).body();
            assertThat(countBooks(list)).isEqualTo(BOOKS);
            assertThat(list).doesNotContain("ffffffff-0000");
        } finally {
            assertThat(app.stop()).isIn(0, 143);
        }

        System.out.println("HOT JOURNAL (i) accepted; journal present after run=" + Files.exists(
                HotJournalWriter.journalOf(file)) + " sha_after_run=" + FileFacts.sha256(file));
        assertThat(Files.exists(HotJournalWriter.journalOf(file))).isFalse();
        assertThat(FileFacts.sha256(file)).isEqualTo(reference);
    }

    @Test
    void invalidFileWithAGenuineHotJournalIsRefusedAndNothingIsWrittenAfterTheRollback() throws Exception {
        newLibrary(true);
        String reference = leaveJournalAndReferenceHash();
        Path console = temp.resolve("console.log");

        int exit = runRefused(console);

        String text = Files.readString(console);
        String after = FileFacts.sha256(file);
        System.out.println("HOT JOURNAL (ii) exit=" + exit + " reason=\"" + Reason.SCHEMA_DIFFERS.text()
                + "\" marker_on_console=" + text.contains(StoredLibraryFixtures.MARKER)
                + " sha_after_reference_rollback=" + reference + " sha_after_refusal=" + after
                + " journal present=" + Files.exists(HotJournalWriter.journalOf(file)));
        assertThat(exit).isNotZero();
        ConsoleRules.assertFixedAndClean(text, Reason.SCHEMA_DIFFERS.text() + ".", temp, Path.of("library.db"));
        assertThat(after).isEqualTo(reference);
        assertThat(Files.exists(HotJournalWriter.journalOf(file))).isFalse();
    }

    @Test
    void garbageJournalBesideAValidFileIsIgnoredAndTheBytesAreUnchanged() throws Exception {
        newLibrary(false);
        String before = FileFacts.sha256(file);
        plantGarbageJournal(file);
        AppProcess app = AppProcess.start("jdbc:sqlite:" + file, "022", null, temp.resolve("console.log"));
        try {
            assertThat(countBooks(app.api().call("GET", "/api/v1/books", null).body())).isEqualTo(BOOKS);
        } finally {
            assertThat(app.stop()).isIn(0, 143);
        }

        System.out.println("GARBAGE JOURNAL (valid) sha_before=" + before + " sha_after=" + FileFacts.sha256(file)
                + " journal present after run=" + Files.exists(HotJournalWriter.journalOf(file)));
        assertThat(FileFacts.sha256(file)).isEqualTo(before);
    }

    @Test
    void garbageJournalBesideAnInvalidFileIsIgnoredAndTheBytesAreUnchanged() throws Exception {
        newLibrary(true);
        String before = FileFacts.sha256(file);
        plantGarbageJournal(file);
        Path console = temp.resolve("console.log");

        int exit = runRefused(console);

        String text = Files.readString(console);
        System.out.println("GARBAGE JOURNAL (invalid) exit=" + exit + " sha_before=" + before + " sha_after="
                + FileFacts.sha256(file) + " journal present after run=" + Files.exists(
                HotJournalWriter.journalOf(file)));
        assertThat(exit).isNotZero();
        ConsoleRules.assertFixedAndClean(text, Reason.SCHEMA_DIFFERS.text() + ".", temp, Path.of("library.db"));
        assertThat(FileFacts.sha256(file)).isEqualTo(before);
    }

    @Test
    void garbageJournalBesideANonDatabaseIsNeverOpenedSoBothFilesAreUnchanged() throws Exception {
        newLibrary(false);
        Files.writeString(file, "not a database " + StoredLibraryFixtures.MARKER);
        plantGarbageJournal(file);
        List<String> before = TreeSnapshot.snapshot(root);
        Path console = temp.resolve("console.log");

        int exit = runRefused(console);

        ConsoleRules.assertFixedAndClean(Files.readString(console), Reason.NOT_A_DATABASE.text() + ".", temp,
                Path.of("library.db"));
        assertThat(exit).isNotZero();
        assertThat(TreeSnapshot.snapshot(root)).isEqualTo(before);
    }
}
