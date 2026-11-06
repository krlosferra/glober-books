package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.FileIdentity;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Guarded;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Reason;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Refusal;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves the comparison of the remembered file identity with the file after the pool opens
 * (Task 34a-1, criterion 5). The race between check and open is not tested: it needs a test-only
 * switch (A-09) and is a documented limit of the guard.
 */
class SqliteFileIdentityTest {

    @TempDir
    Path temp;

    private Path data;
    private Path file;

    @BeforeEach
    void prepare() throws IOException {
        data = temp.toRealPath().resolve("data");
        file = data.resolve("books.db");
    }

    @Test
    void acceptsTheSameFile() {
        Guarded guarded = DataDirectoryGuard.check(file);

        assertThatCode(guarded::requireSameFile).doesNotThrowAnyException();
    }

    @Test
    void refusesAFileSwappedForAnotherFile() throws Exception {
        Guarded guarded = DataDirectoryGuard.check(file);
        swapInAnotherFile();

        assertThatThrownBy(guarded::requireSameFile).isInstanceOf(Refusal.class)
                .hasMessage(Reason.IDENTITY_CHANGED.text());
    }

    @Test
    void refusesAFileReplacedByASymbolicLinkToTheOriginal() throws Exception {
        Guarded guarded = DataDirectoryGuard.check(file);
        Path moved = Files.move(file, data.resolve("moved.db"));
        Files.createSymbolicLink(file, moved);

        assertThat(refusalOf(guarded).reason()).isEqualTo(Reason.IDENTITY_CHANGED);
    }

    @Test
    void refusesAFileThatIsGone() throws Exception {
        Guarded guarded = DataDirectoryGuard.check(file);
        Files.delete(file);

        assertThat(refusalOf(guarded).reason()).isEqualTo(Reason.IDENTITY_CHANGED);
    }

    @Test
    void identitiesOfTwoDistinctFilesDiffer() throws Exception {
        DataDirectoryGuard.check(file);
        Path other = Files.createFile(data.resolve("other.db"));

        FileIdentity first = FileIdentity.of(file);
        FileIdentity second = FileIdentity.of(other);

        assertThat(first).isEqualTo(FileIdentity.of(file)).isNotEqualTo(second);
        assertThat(first.regularFile()).isTrue();
        assertThat(second.regularFile()).isTrue();
    }

    @Test
    void providerStartsOnAGuardedFileAndAppliesTheSchema() throws Exception {
        Guarded guarded = DataDirectoryGuard.check(file);

        try (HikariDataSource pool = SqliteDataSourceProvider.create(guarded)) {
            assertThat(pool.isRunning()).isTrue();
        }
        assertThat(Files.size(file)).isPositive();
    }

    @Test
    void providerRefusesAndReleasesTheClaimWhenTheFileWasSwappedAfterTheCheck() throws Exception {
        Guarded guarded = DataDirectoryGuard.check(file);
        swapInAnotherFile();

        assertThatThrownBy(() -> SqliteDataSourceProvider.create(guarded)).isInstanceOf(Refusal.class)
                .hasMessage(Reason.IDENTITY_CHANGED.text());

        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            assertThat(pool.isRunning()).as("the refused pool was closed and its claim released").isTrue();
        }
    }

    private void swapInAnotherFile() throws IOException {
        Path other = Files.createFile(data.resolve("other.db"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.move(other, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Refusal refusalOf(Guarded guarded) {
        try {
            guarded.requireSameFile();
        } catch (Refusal refusal) {
            return refusal;
        }
        throw new AssertionError("The identity was accepted");
    }
}
