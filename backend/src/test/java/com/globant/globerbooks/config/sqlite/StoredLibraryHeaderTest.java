package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.sqlite.StoredLibraryValidator.Reason;
import com.globant.globerbooks.support.HotJournalWriter;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Follow-up of Task 34b (SEC-F-189, SEC-F-190): the pre-open checks of {@link StoredLibraryHeader} for
 * companion files and an invalid page-count field, with the hot-journal exception kept.
 */
class StoredLibraryHeaderTest {

    @TempDir
    Path temp;

    private Path file;

    private Reason refusal() {
        try {
            StoredLibraryHeader.check(file);
            return null;
        } catch (StoredLibraryValidator.Refusal refusal) {
            return refusal.reason();
        }
    }

    private void library() throws Exception {
        file = StoredLibraryFixtures.library(temp.resolve("library.db"), 3);
    }

    @Test
    void refusesAWalOrShmFileWhateverTheJournalState() throws Exception {
        library();
        assertThat(refusal()).isNull();
        Path wal = file.resolveSibling("library.db-wal");
        Files.write(wal, new byte[] {1});
        assertThat(refusal()).isEqualTo(Reason.WAL_MARKED);
        Files.write(HotJournalWriter.journalOf(file), new byte[] {1});
        assertThat(refusal()).as("a journal does not excuse a -wal").isEqualTo(Reason.WAL_MARKED);
        Files.delete(wal);
        Files.delete(HotJournalWriter.journalOf(file));
        Files.write(file.resolveSibling("library.db-shm"), new byte[] {1});
        assertThat(refusal()).isEqualTo(Reason.WAL_MARKED);
    }

    @Test
    void refusesALinkedWalWithoutFollowingIt() throws Exception {
        library();
        Files.createSymbolicLink(file.resolveSibling("library.db-wal"), temp.resolve("missing"));

        assertThat(refusal()).isEqualTo(Reason.WAL_MARKED);
    }

    @Test
    void refusesAZeroLengthFileBesideAWalFile() throws Exception {
        file = StoredLibraryFixtures.emptyFile(temp.resolve("library.db"));
        Files.write(file.resolveSibling("library.db-wal"), new byte[] {1});

        assertThat(refusal()).isEqualTo(Reason.WAL_MARKED);
    }

    @Test
    void refusesAPageCountWhoseValidFieldDiffersAndAPageCountOfZero() throws Exception {
        library();
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(24);
            raw.write(new byte[] {9, 9, 9, 9});
        }
        assertThat(refusal()).isEqualTo(Reason.DAMAGED);

        Files.delete(file);
        library();
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(28);
            raw.write(new byte[4]);
        }
        assertThat(refusal()).isEqualTo(Reason.DAMAGED);
    }

    @Test
    void nonEmptyJournalStillSkipsTheSizeAndPageCountTests() throws Exception {
        library();
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(28);
            raw.write(new byte[4]);
        }
        Files.write(HotJournalWriter.journalOf(file), new byte[] {1, 2, 3});

        assertThat(refusal()).isNull();
    }
}
