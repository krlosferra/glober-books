package com.globant.globerbooks.config.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteOpenMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Builds the single-connection SQLite pool of the application.
 *
 * <p>The directory (0700), the file (0600) and the private native-library directory
 * ({@code <data dir>/tmp}, 0700) are created before the first connection. Every physical connection
 * receives the fixed settings and then the claim ({@code BEGIN EXCLUSIVE}, {@code COMMIT}, no
 * change), so a second process fails with "database is locked". After the claim this class only
 * asks for attributes of the database path; it never reads, hashes or copies the file, because
 * closing any other descriptor of the file would release the POSIX lock.
 */
public final class SqliteDataSourceProvider {

    private static final String URL_PREFIX = "jdbc:sqlite:";
    private static final String TMPDIR_PROPERTY = "org.sqlite.tmpdir";
    private static final String TMP_DIRECTORY = "tmp";
    private static final String DIRECTORY_MODE = "rwx------";
    private static final String FILE_MODE = "rw-------";
    private static final String SCHEMA_RESOURCE = "schema.sql";
    private static final int BUSY_TIMEOUT_MILLIS = 5000;
    private static final String SQL_TRUSTED_SCHEMA_OFF = "PRAGMA trusted_schema = OFF";
    private static final String SQL_QUERY_ONLY_ON = "PRAGMA query_only = ON";
    private static final String SQL_QUERY_ONLY_OFF = "PRAGMA query_only = OFF";
    private static final String SQL_CLAIM_BEGIN = "BEGIN EXCLUSIVE";
    private static final String SQL_CLAIM_COMMIT = "COMMIT";
    private static final String SQL_JOURNAL_MODE = "PRAGMA journal_mode";
    private static final String DEFAULT_JOURNAL_MODE = "delete";

    private SqliteDataSourceProvider() {
    }

    /**
     * Plain pool for tests of the store itself, not reachable from the application configuration
     * (package-private on purpose, Task 34a-2). Creates the pool from a JDBC URL of the form
     * {@code jdbc:sqlite:<file>}. The form itself is
     * checked by the start policy of a later task; the text after the prefix is taken as given.
     *
     * @param jdbcUrl the value of {@code spring.datasource.url}
     * @return the started pool of one connection, with the schema applied
     */
    static HikariDataSource create(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(URL_PREFIX)) {
            throw new IllegalStateException("The database URL is not a SQLite URL");
        }
        return create(Path.of(jdbcUrl.substring(URL_PREFIX.length())));
    }

    /**
     * Plain pool for tests of the store itself, not guarded and not reachable from the application
     * configuration. Prepares the storage, starts the pool, takes the claim and applies {@code schema.sql}.
     *
     * @param databaseFile the database file; its directory is created when missing
     * @return the started pool of one connection, with the schema applied
     */
    static HikariDataSource create(Path databaseFile) {
        Path absoluteFile = databaseFile.toAbsolutePath();
        String fileName = String.valueOf(absoluteFile.getFileName());
        Path nativeDirectory = prepareStorage(absoluteFile, fileName);
        System.setProperty(TMPDIR_PROPERTY, nativeDirectory.toString());
        HikariDataSource pool = openPool(absoluteFile, fileName);
        try {
            applySchema(pool);
        } catch (SQLException | RuntimeException failure) {
            pool.close();
            throw new IllegalStateException("The database schema could not be applied to " + fileName, failure);
        }
        return pool;
    }

    /**
     * Starts the pool on a location vetted by {@link DataDirectoryGuard}: the guard has created or
     * accepted the directory, the file and the native directory, and after the pool opens the file
     * at the path must still be the file that was vetted. This is the only way the application builds
     * its data source. A failure to open, claim or prepare the database is reported as a refusal with
     * a fixed reason: no path, no file name and no cause.
     *
     * @param guarded the result of {@link DataDirectoryGuard#check(Path)}
     * @return the started pool of one connection, with the schema applied
     * @throws DataDirectoryGuard.Refusal with the identity reason when the file was swapped, or with
     *     the unavailable reason when the database cannot be opened, claimed or prepared
     */
    public static HikariDataSource create(DataDirectoryGuard.Guarded guarded) {
        Path absoluteFile = guarded.databaseFile();
        String fileName = String.valueOf(absoluteFile.getFileName());
        System.setProperty(TMPDIR_PROPERTY, guarded.nativeDirectory().toString());
        boolean emptyFile = StoredLibraryHeader.check(absoluteFile);
        HikariDataSource pool;
        try {
            pool = openPool(absoluteFile, fileName);
        } catch (IllegalStateException failure) {
            throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.DATABASE_UNAVAILABLE);
        } finally {
            NativeDirectoryModes.narrow(guarded.nativeDirectory());
        }
        try {
            guarded.requireSameFile();
            validateStoredLibrary(pool, emptyFile);
            applySchema(pool);
        } catch (DataDirectoryGuard.Refusal | StoredLibraryValidator.Refusal refusal) {
            pool.close();
            throw refusal;
        } catch (SQLException | RuntimeException failure) {
            pool.close();
            throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.DATABASE_UNAVAILABLE);
        }
        return pool;
    }

    private static void validateStoredLibrary(HikariDataSource pool, boolean emptyFile) throws SQLException {
        if (emptyFile) {
            return;
        }
        try (Connection connection = pool.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(SQL_QUERY_ONLY_ON);
            try {
                StoredLibraryValidator.validate(connection);
            } finally {
                statement.execute(SQL_QUERY_ONLY_OFF);
            }
        }
    }

    private static HikariDataSource openPool(Path absoluteFile, String fileName) {
        try {
            return new HikariDataSource(poolConfig(absoluteFile));
        } catch (RuntimeException failure) {
            throw new IllegalStateException("The database " + fileName + " could not be opened and claimed", failure);
        }
    }

    private static Path prepareStorage(Path absoluteFile, String fileName) {
        try {
            Path directory = Objects.requireNonNull(absoluteFile.getParent());
            Path nativeDirectory = directory.resolve(TMP_DIRECTORY);
            createDirectory(directory);
            createDirectory(nativeDirectory);
            if (!Files.exists(absoluteFile)) {
                Files.createFile(absoluteFile, PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString(FILE_MODE)));
            }
            return nativeDirectory;
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("The storage of " + fileName + " could not be prepared");
        }
    }

    private static void createDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            return;
        }
        Path parent = directory.getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            createDirectory(parent);
        }
        Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString(DIRECTORY_MODE)));
    }

    private static HikariConfig poolConfig(Path absoluteFile) {
        HikariConfig config = new HikariConfig();
        config.setDataSource(new ClaimingDataSource(sqliteConfig(), URL_PREFIX + absoluteFile));
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setIdleTimeout(0);
        config.setKeepaliveTime(0);
        config.setMaxLifetime(0);
        return config;
    }

    private static SQLiteConfig sqliteConfig() {
        SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        config.enforceForeignKeys(true);
        config.enableLoadExtension(false);
        config.resetOpenMode(SQLiteOpenMode.CREATE);
        return config;
    }

    private static void applySchema(HikariDataSource pool) throws SQLException {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(SCHEMA_RESOURCE));
        try (Connection connection = pool.getConnection()) {
            populator.populate(connection);
        }
    }

    /**
     * SQLite data source that finishes the initialization of every physical connection: the
     * settings that have no {@link SQLiteConfig} key, the check of the journal mode, and the
     * claim as the last step.
     */
    private static final class ClaimingDataSource extends SQLiteDataSource {

        ClaimingDataSource(SQLiteConfig config, String url) {
            super(config);
            setUrl(url);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return initialize(super.getConnection());
        }

        @Override
        public SQLiteConnection getConnection(String username, String password) throws SQLException {
            return initialize(super.getConnection(username, password));
        }

        private static <T extends Connection> T initialize(T connection) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                statement.execute(SQL_TRUSTED_SCHEMA_OFF);
                statement.execute(SQL_CLAIM_BEGIN);
                statement.execute(SQL_CLAIM_COMMIT);
                requireRollbackJournal(statement);
                return connection;
            } catch (SQLException | RuntimeException failure) {
                closeQuietly(connection);
                throw failure;
            }
        }

        private static void requireRollbackJournal(Statement statement) throws SQLException {
            try (ResultSet result = statement.executeQuery(SQL_JOURNAL_MODE)) {
                if (!result.next() || !DEFAULT_JOURNAL_MODE.equals(result.getString(1).toLowerCase(Locale.ROOT))) {
                    throw new SQLException("The database does not use the default rollback journal");
                }
            }
        }

        private static void closeQuietly(Connection connection) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // the original failure is the one reported
                return;
            }
        }
    }
}
