package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.AppProcess;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Task 34b, criterion 4 on the packaged jar (US-009.3, US-009.5, DEC-028): an absent or zero-length file is
 * created or initialized (mode 0600, directory 0700, the Book table exactly as {@code schema.sql}); a valid
 * database, including one with exactly 5,000 books, opens and no book is altered (a hash of all rows, and of
 * the whole file, is the same after the run).
 */
class StoredLibraryAcceptIntegrationTest {

    @TempDir
    Path temp;

    private Path data;

    private Path newData() throws Exception {
        Path root = Files.createDirectory(temp.resolve("loc"), FileFacts.dirAttr("rwx------"));
        data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        return data;
    }

    private void startAndStop(Path file, int expectedBooks) throws Exception {
        AppProcess app = AppProcess.start("jdbc:sqlite:" + file, "022", null, temp.resolve("console.log"));
        try {
            String body = app.api().call("GET", "/api/v1/books", null).body();
            assertThat(body.split("\"id\"", -1).length - 1).isEqualTo(expectedBooks);
        } finally {
            assertThat(app.stop()).isIn(0, 143);
        }
    }

    private String storedDdl(Path file) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT sql FROM sqlite_master WHERE name = 'books'")) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private void assertInitialized(Path file) throws Exception {
        assertThat(FileFacts.mode(file)).isEqualTo("rw-------");
        assertThat(FileFacts.mode(data)).isEqualTo("rwx------");
        assertThat(StoredLibraryValidator.normalise(storedDdl(file)))
                .isEqualTo(StoredLibraryValidator.normalise(StoredLibraryFixtures.schema()));
    }

    @Test
    void createsTheDatabaseWhenTheFileIsAbsent() throws Exception {
        Path file = newData().resolve("library.db");
        assertThat(file).doesNotExist();

        startAndStop(file, 0);

        System.out.println("ABSENT FILE created: mode=" + FileFacts.mode(file) + " size=" + Files.size(file));
        assertInitialized(file);
    }

    @Test
    void initializesAZeroLengthFile() throws Exception {
        Path file = StoredLibraryFixtures.emptyFile(newData().resolve("library.db"));
        assertThat(Files.size(file)).isZero();

        startAndStop(file, 0);

        System.out.println("EMPTY FILE initialized: mode=" + FileFacts.mode(file) + " size=" + Files.size(file));
        assertInitialized(file);
    }

    @Test
    void opensAValidDatabaseAndAltersNoBook() throws Exception {
        Path file = StoredLibraryFixtures.library(newData().resolve("library.db"), 40);
        String rowsBefore = StoredLibraryFixtures.rowsHash(file);
        String fileBefore = FileFacts.sha256(file);

        startAndStop(file, 40);

        String rowsAfter = StoredLibraryFixtures.rowsHash(file);
        System.out.println("VALID DATABASE rows_sha_before=" + rowsBefore + " rows_sha_after=" + rowsAfter
                + " file_sha_before=" + fileBefore + " file_sha_after=" + FileFacts.sha256(file));
        assertThat(rowsAfter).isEqualTo(rowsBefore);
        assertThat(FileFacts.sha256(file)).isEqualTo(fileBefore);
    }

    @Test
    void opensADatabaseWithExactly5000Books() throws Exception {
        Path file = StoredLibraryFixtures.library(newData().resolve("library.db"), 5000);
        String rowsBefore = StoredLibraryFixtures.rowsHash(file);

        startAndStop(file, 5000);

        assertThat(StoredLibraryFixtures.rowsHash(file)).isEqualTo(rowsBefore);
    }
}
