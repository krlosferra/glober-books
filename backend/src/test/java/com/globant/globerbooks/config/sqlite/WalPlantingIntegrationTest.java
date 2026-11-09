package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.sqlite.StoredLibraryValidator.Reason;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.ConsoleRules;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.HotJournalWriter;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import com.globant.globerbooks.support.TreeSnapshot;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Follow-up of Task 34b, SEC-F-189 and SEC-F-190 (SC-32, SC-39, SEC-F-074, R-20) on the packaged jar. A
 * planted {@code -journal} whose page 1 carries the WAL marker would turn the file into a WAL database after
 * the header was read, and a planted {@code -wal} would then be recovered into the main file: the start must
 * refuse, with the WAL_MARKED reason, before the pool opens, and leave the main file and the {@code -wal}
 * byte-identical. A header whose page count is invalid is refused before the claim could rewrite page 1.
 */
class WalPlantingIntegrationTest {

    @TempDir
    Path temp;

    private Path root;
    private Path file;

    private void newLibrary() throws Exception {
        root = Files.createDirectory(temp.resolve("loc"), FileFacts.dirAttr("rwx------"));
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        file = StoredLibraryFixtures.library(data.resolve("library.db"), 5);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET title = '" + StoredLibraryFixtures.MARKER
                + " title' WHERE id = '" + StoredLibraryFixtures.id(1) + "'");
    }

    private Path plantWal() throws Exception {
        Path wal = file.resolveSibling("library.db-wal");
        StoredLibraryFixtures.genuineWal(file, Files.createDirectory(temp.resolve("walwork")), wal);
        return wal;
    }

    private void assertRefusedUnchanged(Reason reason, String label) throws Exception {
        List<String> before = TreeSnapshot.snapshot(root);
        Path console = temp.resolve("console-" + label + ".log");

        int exit = AppProcess.runToExit("jdbc:sqlite:" + file, "022", null, console);

        String text = Files.readString(console);
        System.out.println("WAL/HEADER " + label + " exit=" + exit + " reason=\"" + reason.text()
                + "\" tree_equal=" + before.equals(TreeSnapshot.snapshot(root))
                + " main_sha=" + FileFacts.sha256(file));
        assertThat(exit).isNotZero();
        ConsoleRules.assertFixedAndClean(text, reason.text() + ".", temp, Path.of("library.db"));
        assertThat(TreeSnapshot.snapshot(root)).isEqualTo(before);
    }

    @Test
    void refusesAPlantedJournalWithWalMarkedPageOneAndPlantedWalIsRefusedAndNothingIsWritten() throws Exception {
        newLibrary();
        Path wal = plantWal();
        HotJournalWriter.journalOf(file).toFile().createNewFile();
        StoredLibraryFixtures.journalWithWalMarkedPageOne(file, HotJournalWriter.journalOf(file));
        String mainBefore = FileFacts.sha256(file);
        String walBefore = FileFacts.sha256(wal);
        assertThat(Files.readAllBytes(file)[18]).as("the main file itself still says 1").isEqualTo((byte) 1);

        assertRefusedUnchanged(Reason.WAL_MARKED, "journal-with-wal-bytes-and-wal");

        assertThat(FileFacts.sha256(file)).isEqualTo(mainBefore);
        assertThat(FileFacts.sha256(wal)).isEqualTo(walBefore);
        assertThat(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1))
                .doesNotContain("evil");
    }

    @Test
    void refusesTheSamePlantedWalWithoutTheJournalIsRefusedToo() throws Exception {
        newLibrary();
        Path wal = plantWal();
        String walBefore = FileFacts.sha256(wal);

        assertRefusedUnchanged(Reason.WAL_MARKED, "wal-only");

        assertThat(FileFacts.sha256(wal)).isEqualTo(walBefore);
    }

    @Test
    void refusesAWalMarkedMainFileWithAPlantedWalIsStillRefusedAsBefore() throws Exception {
        newLibrary();
        StoredLibraryFixtures.run(file, false, "PRAGMA journal_mode = WAL");
        plantWal();

        assertRefusedUnchanged(Reason.WAL_MARKED, "wal-marked-main-with-wal");
    }

    @Test
    void refusesAPlantedShmAndASymbolicLinkWal() throws Exception {
        newLibrary();
        Files.write(file.resolveSibling("library.db-shm"), new byte[32]);
        assertRefusedUnchanged(Reason.WAL_MARKED, "shm-only");

        Files.delete(file.resolveSibling("library.db-shm"));
        Files.createSymbolicLink(file.resolveSibling("library.db-wal"), root.resolve("nowhere"));
        assertRefusedUnchanged(Reason.WAL_MARKED, "dangling-symlink-wal");
    }

    @Test
    void refusesAPageCountThatDiffersFromItsValidFieldBeforeAnythingIsWritten() throws Exception {
        newLibrary();
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(24);
            raw.write(ByteBuffer.allocate(4).putInt(0x01020304).array());
            raw.seek(raw.length());
            raw.write(new byte[4096]);
        }

        assertRefusedUnchanged(Reason.DAMAGED, "bytes-24-92-differ");
    }

    @Test
    void refusesAPageCountOfZeroBeforeAnythingIsWritten() throws Exception {
        newLibrary();
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(28);
            raw.write(new byte[4]);
        }

        assertRefusedUnchanged(Reason.DAMAGED, "page-count-zero");
    }
}
