package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.ClockConfig;
import com.globant.globerbooks.support.ChildJvm;
import com.globant.globerbooks.support.ClaimChildMain;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.PoolFacts;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteConnection;

/** Proves the SQLite data source, the claim from a second JVM, the file modes and the clock (Task 03a). */
class SqliteStoreIntegrationTest {

    private static final String VALID_ID = "123e4567-e89b-42d3-a456-426614174000";
    private static final String INSERT_BOOK = "INSERT INTO books (id, title, author, status, date_added) VALUES ('"
            + VALID_ID + "', 'T', 'A', 'unread', '2026-01-01T00:00:00.000Z')";

    @TempDir
    Path temp;

    @Test
    void createsMissingDirectoryAndFileAndNativeDirectoryWithExactModes() throws Exception {
        Path dir = temp.resolve("data");
        Path file = dir.resolve("books.db");

        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            assertThat(pool.isRunning()).isTrue();
            assertThat(FileFacts.mode(dir)).isEqualTo("rwx------");
            assertThat(FileFacts.mode(file)).isEqualTo("rw-------");
            assertThat(FileFacts.mode(dir.resolve("tmp"))).isEqualTo("rwx------");
        }
    }

    @Test
    void keepsEveryFileNextToTheDatabaseNotWiderThan0600AfterAWrite() throws Exception {
        Path dir = temp.resolve("data");
        Path file = dir.resolve("books.db");
        List<String> seen = new ArrayList<>();

        try (HikariDataSource pool = SqliteDataSourceProvider.create(file);
                Connection connection = pool.getConnection();
                Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute(INSERT_BOOK);
            seen.addAll(FileFacts.sidecarsOf(dir, file));
            connection.commit();
            seen.addAll(FileFacts.sidecarsOf(dir, file));
        }

        System.out.println("SIDECARS_SEEN=" + seen);
        assertThat(FileFacts.mode(file)).isEqualTo("rw-------");
        for (Path other : FileFacts.regularFilesNextTo(dir, file)) {
            assertThat(FileFacts.isNotWiderThan0600(other)).as(other.getFileName().toString()).isTrue();
        }
    }

    @Test
    void doesNotChangeModesOfAnExistingDirectoryAndFile() throws Exception {
        Path dir = Files.createDirectory(temp.resolve("existing"), FileFacts.dirAttr("rwxr-x---"));
        Path file = Files.createFile(dir.resolve("books.db"), FileFacts.fileAttr("rw-r-----"));

        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            assertThat(pool.isRunning()).isTrue();
            assertThat(FileFacts.mode(dir)).isEqualTo("rwxr-x---");
            assertThat(FileFacts.mode(file)).isEqualTo("rw-r-----");
        }
    }

    @Test
    void readsBackEverySettingOnTheLiveConnectionAndAfterEviction() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            Map<String, String> before = PoolFacts.settings(pool);
            SQLiteConnection first = PoolFacts.rawConnection(pool);
            pool.getHikariPoolMXBean().softEvictConnections();
            SQLiteConnection second = PoolFacts.awaitNewConnection(pool, first);
            Map<String, String> after = PoolFacts.settings(pool);

            System.out.println("SETTINGS_BEFORE=" + before);
            System.out.println("SETTINGS_AFTER=" + after);
            System.out.println("POOL max=" + pool.getMaximumPoolSize() + " minIdle=" + pool.getMinimumIdle()
                    + " idle=" + pool.getIdleTimeout() + " keepalive=" + pool.getKeepaliveTime()
                    + " maxLifetime=" + pool.getMaxLifetime());
            assertThat(second).isNotSameAs(first);
            assertExpectedSettings(before);
            assertExpectedSettings(after);
            assertThat(pool.getMaximumPoolSize()).isEqualTo(1);
            assertThat(pool.getMinimumIdle()).isEqualTo(1);
            assertThat(pool.getIdleTimeout()).isZero();
            assertThat(pool.getKeepaliveTime()).isZero();
            assertThat(pool.getMaxLifetime()).isZero();
            HikariPoolMXBean bean = pool.getHikariPoolMXBean();
            assertThat(bean.getTotalConnections()).isEqualTo(1);
        }
    }

    @Test
    void failsToLoadAnExtension() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file);
                Connection connection = pool.getConnection();
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.execute("SELECT load_extension('nonexistent_extension')"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("not authorized");
        }
    }

    @Test
    void refusesToStartOnAFileThatIsNotInRollbackJournalMode() throws Exception {
        Path file = temp.resolve("wal").resolve("books.db");
        Files.createDirectories(file.getParent());
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file);
                Connection connection = pool.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
        }

        assertThatThrownBy(() -> SqliteDataSourceProvider.create(file)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secondJvmFailsWithDatabaseIsLockedWhileChildHoldsAndHashIsUnchanged() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            assertThat(pool.isRunning()).isTrue();
        }
        String hashBefore = FileFacts.sha256(file);
        Process holder = null;
        try {
            holder = ChildJvm.startHolder(file);
            Path nativeDir = file.getParent().resolve("tmp");
            try (Stream<Path> libs = Files.list(nativeDir)) {
                assertThat(libs.map(path -> path.getFileName().toString()))
                        .anyMatch(name -> name.contains("sqlitejdbc"));
            }
            ChildJvm.ChildResult refused = ChildJvm.runChild("try", file);
            System.out.println("SECOND_JVM exit=" + refused.exit() + " out=" + refused.output());
            assertThat(refused.exit()).isEqualTo(ClaimChildMain.EXIT_REFUSED);
            assertThat(refused.output()).contains("database is locked");
        } finally {
            ChildJvm.stop(holder);
        }
        String hashAfter = FileFacts.sha256(file);
        System.out.println("SHA256 before=" + hashBefore + " after=" + hashAfter);
        assertThat(hashAfter).isEqualTo(hashBefore);
        ChildJvm.ChildResult control = ChildJvm.runChild("try", file);
        System.out.println("NEGATIVE_CONTROL exit=" + control.exit() + " out=" + control.output());
        assertThat(control.exit()).isZero();
        assertThat(control.output()).contains("CLAIMED");
    }

    @Test
    void secondJvmStillFailsWhileTheReplacementConnectionAfterEvictionHoldsTheClaim() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            SQLiteConnection first = PoolFacts.rawConnection(pool);
            pool.getHikariPoolMXBean().softEvictConnections();
            SQLiteConnection second = PoolFacts.awaitNewConnection(pool, first);

            ChildJvm.ChildResult refused = ChildJvm.runChild("try", file);

            System.out.println("SECOND_JVM_AFTER_EVICTION exit=" + refused.exit() + " out=" + refused.output());
            assertThat(second).isNotSameAs(first);
            assertThat(refused.exit()).isEqualTo(ClaimChildMain.EXIT_REFUSED);
            assertThat(refused.output()).contains("database is locked");
        }
    }

    @Test
    void failsStartWithFixedMessageWhenAnotherProcessHoldsTheFile() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");
        Process holder = null;
        try {
            holder = ChildJvm.startHolder(file);

            assertThatThrownBy(() -> SqliteDataSourceProvider.create(file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("The database books.db could not be opened and claimed");
        } finally {
            ChildJvm.stop(holder);
        }
    }

    @Test
    void failsWithFixedMessageNamingOnlyTheFileWhenParentIsNotADirectory() throws Exception {
        Path blocker = Files.createFile(temp.resolve("blocker"));
        Path file = blocker.resolve("data").resolve("books.db");

        assertThatThrownBy(() -> SqliteDataSourceProvider.create(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The storage of books.db could not be prepared")
                .hasNoCause();
    }

    @Test
    void failsWithFixedMessageWhenParentIsNotWritable() throws Exception {
        Path locked = Files.createDirectory(temp.resolve("locked"), FileFacts.dirAttr("r-x------"));
        Assumptions.assumeFalse(Files.isWritable(locked), "running as a user that ignores permissions");
        Path file = locked.resolve("data").resolve("books.db");

        assertThatThrownBy(() -> SqliteDataSourceProvider.create(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The storage of books.db could not be prepared");
    }

    @Test
    void appliesSchemaAtEveryStartWithoutChangingTheStoredTable() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");
        String first;
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            first = PoolFacts.storedDdl(pool);
        }
        String second;
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            second = PoolFacts.storedDdl(pool);
        }

        assertThat(first).startsWith("CREATE TABLE books").contains("ck_books_rating", "STRICT");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void acceptsAJdbcUrlOfTheFixedForm() throws Exception {
        Path file = temp.resolve("data").resolve("books.db");

        try (HikariDataSource pool = SqliteDataSourceProvider.create("jdbc:sqlite:" + file)) {
            assertThat(pool.isRunning()).isTrue();
        }
    }

    @Test
    void rejectsAnUrlThatIsNotSqlite() {
        assertThatThrownBy(() -> SqliteDataSourceProvider.create("jdbc:h2:mem:x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void providesAClockInUtc() {
        Clock clock = new ClockConfig().clock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void mainSourcesCallTheWallClockOnlyInClockConfig() throws IOException {
        Pattern wallClock = Pattern.compile(
                "Clock\\.systemUTC|Instant\\.now|LocalDate(Time)?\\.now|new Date\\(|currentTimeMillis");
        List<String> hits = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(Path.of("src", "main", "java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source, StandardCharsets.UTF_8);
                if (wallClock.matcher(text.replaceAll("(?s)/\\*.*?\\*/", "")).find()) {
                    hits.add(source.getFileName().toString());
                }
            }
        }

        assertThat(hits).containsExactly("ClockConfig.java");
    }

    private static void assertExpectedSettings(Map<String, String> values) {
        assertThat(values).containsEntry("journal_mode", "delete").containsEntry("synchronous", "2")
                .containsEntry("locking_mode", "exclusive").containsEntry("foreign_keys", "1")
                .containsEntry("busy_timeout", "5000").containsEntry("trusted_schema", "0");
    }
}
