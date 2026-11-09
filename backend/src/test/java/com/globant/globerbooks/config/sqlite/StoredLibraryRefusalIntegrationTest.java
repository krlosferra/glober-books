package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.sqlite.StoredLibraryValidator.Reason;
import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.ConsoleRules;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import com.globant.globerbooks.support.TreeSnapshot;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34b, criteria 1, 2 and SEC-F-181 on the packaged jar (US-009.5 as changed by CR-9, DEC-028, SC-32,
 * SC-39, SEC-F-068): every listed invalid stored library makes the start end with a non-zero exit and the
 * fixed reason of its category (and of no other category), the console shows no path, no stack trace and no
 * book data (a recognizable marker is planted in the invalid row and is first shown to be in the file), and
 * the whole tree, with the SHA-256 of the main file, is the same after the refusal as before.
 */
class StoredLibraryRefusalIntegrationTest {

    private static final String MARK = StoredLibraryFixtures.MARKER;

    @TempDir
    Path temp;

    private Path root;
    private Path file;

    private Path newLibrary(int books) throws Exception {
        root = Files.createDirectory(temp.resolve("loc"), FileFacts.dirAttr("rwx------"));
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        file = data.resolve("library.db");
        return StoredLibraryFixtures.library(file, books);
    }

    private void refuse(String label, Reason expected, boolean markerPlanted) throws Exception {
        List<String> before = TreeSnapshot.snapshot(root);
        String hashBefore = FileFacts.sha256(file);
        if (markerPlanted) {
            assertThat(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1)).contains(MARK);
        }
        Path console = temp.resolve("console.log");

        int exit = AppProcess.runToExit("jdbc:sqlite:" + file, "022", null, console);

        String text = Files.readString(console);
        String hashAfter = FileFacts.sha256(file);
        System.out.println("REFUSAL " + label + " exit=" + exit + " reason=\"" + expected.text() + "\" marker_in_file="
                + markerPlanted + " marker_on_console=" + text.contains(MARK) + " sha_before=" + hashBefore
                + " sha_after=" + hashAfter);
        assertThat(exit).isNotZero();
        ConsoleRules.assertFixedAndClean(text, expected.text() + ".", temp, Path.of("library.db"));
        Arrays.stream(Reason.values()).filter(other -> other != expected)
                .forEach(other -> assertThat(text).doesNotContain(other.text()));
        assertThat(text).doesNotContain("SQLITE").doesNotContain("SELECT");
        assertThat(hashAfter).isEqualTo(hashBefore);
        assertThat(TreeSnapshot.snapshot(root)).isEqualTo(before);
    }

    private void plantMarker() throws Exception {
        StoredLibraryFixtures.run(file, false, "UPDATE books SET title = '" + MARK + " title' WHERE id = '"
                + StoredLibraryFixtures.id(1) + "'");
    }

    @Test
    void refusesANonDatabase() throws Exception {
        newLibrary(0);
        Files.writeString(file, ("plain text " + MARK + " ").repeat(40));

        refuse("non-database", Reason.NOT_A_DATABASE, true);
    }

    @Test
    void refusesAWalMarkedFile() throws Exception {
        newLibrary(3);
        plantMarker();
        StoredLibraryFixtures.run(file, false, "PRAGMA journal_mode = WAL");

        refuse("wal-marked", Reason.WAL_MARKED, true);
    }

    @Test
    void refusesATruncatedFileByTheHeaderSizeTest() throws Exception {
        newLibrary(400);
        plantMarker();
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.setLength(raw.length() - 4096);
        }

        refuse("damaged: size differs from page count", Reason.DAMAGED, false);
    }

    @Test
    void refusesAFileWithAGarbagePageAndAValidHeader() throws Exception {
        newLibrary(300);
        byte[] garbage = new byte[4096];
        Arrays.fill(garbage, (byte) 0xA7);
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(4096L * 2);
            raw.write(garbage);
        }

        refuse("damaged: garbage page", Reason.DAMAGED, false);
    }

    @Test
    void refusesADatabaseWithNoTables() throws Exception {
        newLibrary(0);
        StoredLibraryFixtures.run(file, false, "DROP TABLE books");

        refuse("no tables", Reason.NO_TABLES, false);
    }

    @Test
    void refusesOtherStructure() throws Exception {
        newLibrary(3);
        plantMarker();
        StoredLibraryFixtures.run(file, false, "CREATE TABLE extra (name TEXT)");

        refuse("other structure: extra table", Reason.SCHEMA_DIFFERS, true);
    }

    @Test
    void refusesATableBooksWithoutItsChecks() throws Exception {
        newLibrary(0);
        StoredLibraryFixtures.run(file, false, "DROP TABLE books", "CREATE TABLE books (id TEXT NOT NULL PRIMARY KEY, "
                + "title TEXT NOT NULL, author TEXT NOT NULL, status TEXT NOT NULL, rating INTEGER, "
                + "date_added TEXT NOT NULL, date_completed TEXT) STRICT",
                "INSERT INTO books VALUES ('x', '" + MARK + "', 'A', 'unread', 9, 'bad', NULL)");

        refuse("books without CHECKs", Reason.SCHEMA_DIFFERS, true);
    }

    @Test
    void refusesAnExtraIndex() throws Exception {
        newLibrary(3);
        StoredLibraryFixtures.run(file, false, "CREATE INDEX idx_title ON books (title)");

        refuse("extra index", Reason.SCHEMA_DIFFERS, false);
    }

    @Test
    void refusesAView() throws Exception {
        newLibrary(3);
        StoredLibraryFixtures.run(file, false, "CREATE VIEW titles AS SELECT title FROM books");

        refuse("view", Reason.FORBIDDEN_OBJECT, false);
    }

    @Test
    void refusesATrigger() throws Exception {
        newLibrary(3);
        StoredLibraryFixtures.run(file, false, "CREATE TRIGGER touch AFTER DELETE ON books BEGIN SELECT 1; END");

        refuse("trigger", Reason.FORBIDDEN_OBJECT, false);
    }

    @Test
    void refusesAVirtualTable() throws Exception {
        newLibrary(3);
        StoredLibraryFixtures.run(file, false, "CREATE VIRTUAL TABLE search USING fts5(body)");

        refuse("virtual table", Reason.FORBIDDEN_OBJECT, false);
    }

    @Test
    void refusesARowThatBreaksARatingInvariant() throws Exception {
        newLibrary(2);
        StoredLibraryFixtures.forceRow(file, StoredLibraryFixtures.id(50), MARK + " title", "Author", "unread", 3,
                StoredLibraryFixtures.DATE, null);

        refuse("INV-1 rating on an unread book (stopped by quick_check)", Reason.DAMAGED, true);
    }

    @Test
    void refusesARowWithoutACompletionDateOnACompletedBook() throws Exception {
        newLibrary(2);
        StoredLibraryFixtures.forceRow(file, StoredLibraryFixtures.id(50), MARK + " title", "Author", "completed", 3,
                StoredLibraryFixtures.DATE, null);

        refuse("INV-4 completed without date (stopped by quick_check)", Reason.DAMAGED, true);
    }

    @Test
    void refusesARowWithATimestampThatIsNotADate() throws Exception {
        newLibrary(2);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET title = '" + MARK + "', date_added = "
                + "'2026-13-45T25:61:61.000Z' WHERE id = '" + StoredLibraryFixtures.id(1) + "'");

        refuse("timestamp format", Reason.INVALID_ROW, true);
    }

    @Test
    void refusesATitleWithAnEmbeddedNul() throws Exception {
        newLibrary(2);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET title = '" + MARK + "' || char(0) || 'tail'");

        refuse("NUL inside a title", Reason.INVALID_ROW, true);
    }

    @Test
    void refusesOneOversizedCell() throws Exception {
        newLibrary(2);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET author = '" + MARK
                + "' || char(0) || replace(hex(zeroblob(5000)), '0', 'x') WHERE id = '"
                + StoredLibraryFixtures.id(1) + "'");

        refuse("one oversized cell", Reason.OVERSIZED_CELL, true);
    }

    @Test
    void refusesMoreThan5000Books() throws Exception {
        newLibrary(5000);
        StoredLibraryFixtures.run(file, false, "INSERT INTO books (id, title, author, status, date_added) VALUES "
                + "('" + StoredLibraryFixtures.id(9999) + "', '" + MARK + "', 'Author', 'unread', '"
                + StoredLibraryFixtures.DATE + "')");

        refuse("5,001 books", Reason.TOO_MANY_BOOKS, true);
    }
}
