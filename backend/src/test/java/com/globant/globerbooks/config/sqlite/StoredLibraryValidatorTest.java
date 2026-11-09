package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.sqlite.StoredLibraryValidator.Reason;
import com.globant.globerbooks.support.ChildJvm;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34b, criteria 1 and 2 at unit level (US-009.5, DEC-028, SEC-F-068, SEC-F-074): the header check and the
 * validator on real SQLite files. The oracle is the spec: every listed case is refused with a fixed reason of
 * its own category, and the accepted cases pass. The same cases on the packaged jar are in
 * {@code StoredLibraryRefusalIntegrationTest}.
 */
class StoredLibraryValidatorTest {

    private static final String MARK = StoredLibraryFixtures.MARKER;

    @TempDir
    Path temp;

    private Path file;

    private Path library(int books) throws Exception {
        file = StoredLibraryFixtures.library(temp.resolve("library.db"), books);
        return file;
    }

    private Reason headerRefusal() {
        try {
            StoredLibraryHeader.check(file);
            return null;
        } catch (StoredLibraryValidator.Refusal refusal) {
            return refusal.reason();
        }
    }

    private Reason validatorRefusal() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file)) {
            StoredLibraryValidator.validate(connection);
            return null;
        } catch (StoredLibraryValidator.Refusal refusal) {
            assertThat(refusal.getMessage()).isEqualTo(refusal.reason().text()).doesNotContain(MARK);
            return refusal.reason();
        }
    }

    @Test
    void acceptsAValidLibraryWithAndWithoutBooks() throws Exception {
        library(0);
        assertThat(headerRefusal()).isNull();
        assertThat(validatorRefusal()).isNull();
        Files.delete(file);
        library(7);
        assertThat(headerRefusal()).isNull();
        assertThat(validatorRefusal()).isNull();
    }

    @Test
    void acceptsExactly5000BooksAndRefuses5001() throws Exception {
        library(5000);
        assertThat(validatorRefusal()).isNull();
        StoredLibraryFixtures.run(file, false, "INSERT INTO books (id, title, author, status, date_added) VALUES "
                + "('" + StoredLibraryFixtures.id(9999) + "', 'One more', 'Author', 'unread', '"
                + StoredLibraryFixtures.DATE + "')");

        assertThat(validatorRefusal()).isEqualTo(Reason.TOO_MANY_BOOKS);
    }

    @Test
    void headerAcceptsAZeroLengthFile() throws Exception {
        file = StoredLibraryFixtures.emptyFile(temp.resolve("library.db"));

        assertThat(headerRefusal()).isNull();
        assertThat(StoredLibraryHeader.check(file)).as("zero length: nothing to validate").isTrue();
        Files.delete(file);
        library(1);
        assertThat(StoredLibraryHeader.check(file)).as("non-empty: validate").isFalse();
    }

    @Test
    void headerRefusesATextFileAndAShortFileAsNotADatabase() throws Exception {
        file = temp.resolve("library.db");
        Files.writeString(file, "This is " + MARK + " and not a database. ".repeat(20));
        assertThat(headerRefusal()).isEqualTo(Reason.NOT_A_DATABASE);

        Files.write(file, "SQLite format 3\0".getBytes(StandardCharsets.ISO_8859_1));
        assertThat(headerRefusal()).isEqualTo(Reason.NOT_A_DATABASE);
    }

    @Test
    void headerRefusesAFileMarkedForWriteAheadLoggingBeforeSqliteOpensIt() throws Exception {
        library(3);
        StoredLibraryFixtures.run(file, false, "PRAGMA journal_mode = WAL");
        byte[] bytes = Files.readAllBytes(file);
        assertThat(bytes[18]).isEqualTo((byte) 2);
        assertThat(bytes[19]).isEqualTo((byte) 2);

        assertThat(headerRefusal()).isEqualTo(Reason.WAL_MARKED);
    }

    @Test
    void headerRefusesAFileWhoseSizeIsNotPageSizeTimesPageCount() throws Exception {
        library(400);
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.setLength(raw.length() - 4096);
        }

        assertThat(headerRefusal()).isEqualTo(Reason.DAMAGED);
    }

    @Test
    void headerSkipsTheSizeTestWhenANonEmptyJournalExistsButNotForAnEmptyOne() throws Exception {
        library(400);
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.setLength(raw.length() - 4096);
        }
        Path journal = file.resolveSibling("library.db-journal");
        Files.createFile(journal);
        assertThat(headerRefusal()).as("empty journal does not skip the test").isEqualTo(Reason.DAMAGED);

        Files.writeString(journal, "journal bytes");
        assertThat(headerRefusal()).as("non-empty journal skips the size test").isNull();
    }

    @Test
    void headerRefusesAnImpossiblePageSizeAndAnUnknownFormatVersion() {
        byte[] header = new byte[100];
        System.arraycopy("SQLite format 3\0".getBytes(StandardCharsets.ISO_8859_1), 0, header, 0, 16);
        header[18] = 1;
        header[19] = 1;
        header[17] = (byte) 200;

        assertThatThrownBy(() -> StoredLibraryHeader.require(header, 100, 100, false))
                .isInstanceOfSatisfying(StoredLibraryValidator.Refusal.class,
                        refusal -> assertThat(refusal.reason()).isEqualTo(Reason.DAMAGED));
        header[17] = 0;
        header[16] = 16;
        header[18] = 3;
        assertThatThrownBy(() -> StoredLibraryHeader.require(header, 100, 100, false))
                .isInstanceOfSatisfying(StoredLibraryValidator.Refusal.class,
                        refusal -> assertThat(refusal.reason()).isEqualTo(Reason.NOT_A_DATABASE));
    }

    @Test
    void headerReportsInUseNotDamagedWhenAnotherProcessHoldsTheClaim() throws Exception {
        library(5);
        Process holder = ChildJvm.startHolder(file);
        try {
            assertThatThrownBy(() -> StoredLibraryHeader.check(file))
                    .isInstanceOfSatisfying(DataDirectoryGuard.Refusal.class, refusal -> assertThat(refusal.reason())
                            .isEqualTo(DataDirectoryGuard.Reason.DATABASE_UNAVAILABLE));
        } finally {
            ChildJvm.stop(holder);
        }
    }

    @Test
    void refusesADatabaseWithNoTables() throws Exception {
        library(0);
        StoredLibraryFixtures.run(file, false, "DROP TABLE books");

        assertThat(validatorRefusal()).isEqualTo(Reason.NO_TABLES);
    }

    @Test
    void refusesOtherStructure() throws Exception {
        library(2);
        StoredLibraryFixtures.run(file, false, "CREATE TABLE extra (name TEXT)");
        assertThat(validatorRefusal()).isEqualTo(Reason.SCHEMA_DIFFERS);

        StoredLibraryFixtures.run(file, false, "DROP TABLE extra", "DROP TABLE books",
                "CREATE TABLE notbooks (id TEXT)");
        assertThat(validatorRefusal()).isEqualTo(Reason.SCHEMA_DIFFERS);
    }

    @Test
    void refusesATableBooksWithoutItsChecksAndWithoutStrict() throws Exception {
        library(0);
        StoredLibraryFixtures.run(file, false, "DROP TABLE books", "CREATE TABLE books (id TEXT NOT NULL PRIMARY KEY, "
                + "title TEXT NOT NULL, author TEXT NOT NULL, status TEXT NOT NULL, rating INTEGER, "
                + "date_added TEXT NOT NULL, date_completed TEXT) STRICT");
        assertThat(validatorRefusal()).isEqualTo(Reason.SCHEMA_DIFFERS);

        String noRatingCheck = StoredLibraryFixtures.schema().replaceAll("(?s)CONSTRAINT ck_books_rating CHECK "
                + "\\(rating IS NULL OR \\(rating BETWEEN 1 AND 5 AND status = 'completed'\\)\\),\\s*", "");
        assertThat(noRatingCheck).doesNotContain("ck_books_rating");
        StoredLibraryFixtures.run(file, false, "DROP TABLE books", noRatingCheck);
        assertThat(validatorRefusal()).as("one missing named CHECK").isEqualTo(Reason.SCHEMA_DIFFERS);
    }

    @Test
    void refusesAnExtraIndex() throws Exception {
        library(3);
        StoredLibraryFixtures.run(file, false, "CREATE INDEX idx_title ON books (title)");

        assertThat(validatorRefusal()).isEqualTo(Reason.SCHEMA_DIFFERS);
    }

    @Test
    void refusesAViewATriggerAndAVirtualTable() throws Exception {
        library(1);
        StoredLibraryFixtures.run(file, false, "CREATE VIEW titles AS SELECT title FROM books");
        assertThat(validatorRefusal()).isEqualTo(Reason.FORBIDDEN_OBJECT);

        StoredLibraryFixtures.run(file, false, "DROP VIEW titles",
                "CREATE TRIGGER touch AFTER DELETE ON books BEGIN SELECT 1; END");
        assertThat(validatorRefusal()).isEqualTo(Reason.FORBIDDEN_OBJECT);

        StoredLibraryFixtures.run(file, false, "DROP TRIGGER touch", "CREATE VIRTUAL TABLE search USING fts5(body)");
        assertThat(validatorRefusal()).isEqualTo(Reason.FORBIDDEN_OBJECT);
    }

    @Test
    void refusesADamagedFileThatHasAValidHeader() throws Exception {
        library(300);
        byte[] garbage = new byte[4096];
        Arrays.fill(garbage, (byte) 0xA7);
        try (RandomAccessFile raw = new RandomAccessFile(file.toFile(), "rw")) {
            raw.seek(4096L * 2);
            raw.write(garbage);
        }
        assertThat(headerRefusal()).isNull();

        assertThat(validatorRefusal()).isEqualTo(Reason.DAMAGED);
    }

    @Test
    void refusesRowsThatBreakAChecksConstraintAsDamagedBecauseQuickCheckRunsFirst() throws Exception {
        library(2);
        StoredLibraryFixtures.forceRow(file, StoredLibraryFixtures.id(50), MARK + " title", "Author", "unread", 3,
                StoredLibraryFixtures.DATE, null);

        assertThat(validatorRefusal()).isEqualTo(Reason.DAMAGED);
    }

    @Test
    void refusesATitleWithAnEmbeddedNulThatPassesTheLengthCheck() throws Exception {
        library(2);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET title = '" + MARK + "' || char(0) || 'tail'");

        assertThat(validatorRefusal()).isEqualTo(Reason.INVALID_ROW);
    }

    @Test
    void refusesOneOversizedCellBeforeItsTextIsFetched() throws Exception {
        library(2);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET author = '" + MARK
                + "' || char(0) || replace(hex(zeroblob(5000)), '0', 'x') WHERE id = '"
                + StoredLibraryFixtures.id(1) + "'");

        assertThat(validatorRefusal()).isEqualTo(Reason.OVERSIZED_CELL);
    }

    @Test
    void refusesATimestampThatMatchesTheGlobButIsNotADate() throws Exception {
        library(2);
        StoredLibraryFixtures.run(file, false, "UPDATE books SET date_added = '2026-13-45T25:61:61.000Z' "
                + "WHERE id = '" + StoredLibraryFixtures.id(1) + "'");

        assertThat(validatorRefusal()).isEqualTo(Reason.INVALID_ROW);
    }
}
