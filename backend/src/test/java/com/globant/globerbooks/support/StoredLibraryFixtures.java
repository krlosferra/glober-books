package com.globant.globerbooks.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Builds database files for the stored-library tests with plain JDBC (never through the application). The
 * file is created with mode 0600 before the driver opens it, so the guard accepts it. Rows can be forced past
 * the CHECK constraints with {@code ignore_check_constraints}, to make the invalid rows the validator must
 * refuse. Recognizable marker text lets the console and log checks prove that no book data is printed.
 */
public final class StoredLibraryFixtures {

    /** Marker placed in invalid rows; {@code ConsoleRules} requires that it never reaches the console. */
    public static final String MARKER = "ZQXDATA";
    /** A valid creation timestamp. */
    public static final String DATE = "2026-01-02T03:04:05.678Z";

    private static final String INSERT = "INSERT INTO books (id, title, author, status, rating, date_added, "
            + "date_completed) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private StoredLibraryFixtures() {
    }

    /**
     * Returns the text of {@code schema.sql}.
     *
     * @return the DDL
     * @throws IOException when the resource cannot be read
     */
    public static String schema() throws IOException {
        try (InputStream stream = StoredLibraryFixtures.class.getClassLoader().getResourceAsStream("schema.sql")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns the id of the book with this index.
     *
     * @param index the index
     * @return a lower-case UUID text
     */
    public static String id(int index) {
        return String.format("00000000-0000-4000-8000-%012x", index);
    }

    /**
     * Creates an empty file with mode 0600.
     *
     * @param file the file
     * @return the file
     * @throws IOException when it cannot be created
     */
    public static Path emptyFile(Path file) throws IOException {
        return Files.createFile(file, FileFacts.fileAttr("rw-------"));
    }

    /**
     * Creates a valid library of the given number of books (status unread, one completed with rating 4 every
     * third book).
     *
     * @param file the database file (created with mode 0600)
     * @param books the number of books
     * @return the file
     * @throws Exception when the database cannot be written
     */
    public static Path library(Path file, int books) throws Exception {
        emptyFile(file);
        run(file, false, schema());
        try (Connection connection = open(file, false);
                PreparedStatement insert = connection.prepareStatement(INSERT)) {
            connection.setAutoCommit(false);
            for (int index = 0; index < books; index++) {
                boolean done = index % 3 == 0;
                insertRow(insert, id(index), "Title " + index, "Author " + index, done ? "completed" : "unread",
                        done ? 4 : null, DATE, done ? DATE : null);
            }
            connection.commit();
        }
        return file;
    }

    /**
     * Inserts one row as given, bypassing the CHECK constraints.
     *
     * @param file the database file
     * @param id the id
     * @param title the title
     * @param author the author
     * @param status the status
     * @param rating the rating or null
     * @param added the creation timestamp
     * @param completed the completion timestamp or null
     * @throws Exception when the database cannot be written
     */
    public static void forceRow(Path file, String id, String title, String author, String status, Integer rating,
            String added, String completed) throws Exception {
        try (Connection connection = open(file, true);
                PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insertRow(insert, id, title, author, status, rating, added, completed);
        }
    }

    /**
     * Runs statements on the file, optionally with the CHECK constraints ignored.
     *
     * @param file the database file
     * @param ignoreChecks whether {@code ignore_check_constraints} is on
     * @param statements the statements
     * @throws Exception when the database cannot be written
     */
    public static void run(Path file, boolean ignoreChecks, String... statements) throws Exception {
        try (Connection connection = open(file, ignoreChecks); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static Connection open(Path file, boolean ignoreChecks) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        if (ignoreChecks) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA ignore_check_constraints = ON");
            }
        }
        return connection;
    }

    private static void insertRow(PreparedStatement insert, String id, String title, String author, String status,
            Integer rating, String added, String completed) throws SQLException {
        insert.setString(1, id);
        insert.setString(2, title);
        insert.setString(3, author);
        insert.setString(4, status);
        insert.setObject(5, rating);
        insert.setString(6, added);
        insert.setString(7, completed);
        insert.executeUpdate();
    }

    /**
     * Returns a SHA-256 over every row in id order, read through a plain connection (for the "no book is
     * altered" proofs; call it only while no application holds the file).
     *
     * @param file the database file
     * @return the hex digest
     * @throws Exception when the rows cannot be read
     */
    public static String rowsHash(Path file) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        try (Connection connection = open(file, false); Statement statement = connection.createStatement();
                java.sql.ResultSet rows = statement.executeQuery("SELECT id, title, author, status, rating, "
                        + "date_added, date_completed FROM books ORDER BY id")) {
            while (rows.next()) {
                for (int column = 1; column <= 7; column++) {
                    digest.update(String.valueOf(rows.getString(column)).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                }
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Produces a genuine {@code -wal} file: a copy of the library is switched to write-ahead logging with
     * automatic checkpoints off, a table {@code evil} is created, and while that connection is still open the
     * {@code -wal} file is copied to the target.
     *
     * @param library an existing library file
     * @param scratch an existing empty directory for the working copy
     * @param target where the copy of the {@code -wal} file goes
     * @throws Exception when the database cannot be written
     */
    public static void genuineWal(Path library, Path scratch, Path target) throws Exception {
        Path work = scratch.resolve("work.db");
        Files.copy(library, work);
        try (Connection connection = open(work, false); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA wal_autocheckpoint = 0");
            statement.execute("CREATE TABLE evil (x TEXT)");
            statement.execute("INSERT INTO evil VALUES ('planted')");
            Files.copy(scratch.resolve("work.db-wal"), target);
        }
    }

    /**
     * Writes a hand-built, valid rollback journal for the file whose only record is page 1 with its WAL
     * marker (bytes 18 and 19 equal to 2). Bytes 18 and 19 lie outside the journal checksum sample, so the
     * checksum is the normal one. Rolling it back turns a rollback-journal file into a WAL-marked one.
     *
     * @param file the database file (4,096-byte pages)
     * @param journal the journal file to write
     * @throws Exception when the file cannot be read or the journal written
     */
    public static void journalWithWalMarkedPageOne(Path file, Path journal) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        int pageSize = 4096;
        byte[] page = Arrays.copyOf(bytes, pageSize);
        page[18] = 2;
        page[19] = 2;
        int nonce = 0x1234ABCD;
        int checksum = nonce;
        for (int index = pageSize - 200; index > 0; index -= 200) {
            checksum += page[index] & 0xFF;
        }
        ByteBuffer out = ByteBuffer.allocate(512 + 4 + pageSize + 4);
        out.put(new byte[] {(byte) 0xd9, (byte) 0xd5, 0x05, (byte) 0xf9, 0x20, (byte) 0xa1, 0x63, (byte) 0xd7});
        out.putInt(1).putInt(nonce).putInt(bytes.length / pageSize).putInt(512).putInt(pageSize);
        out.position(512);
        out.putInt(1).put(page).putInt(checksum);
        Files.write(journal, out.array());
    }
}
