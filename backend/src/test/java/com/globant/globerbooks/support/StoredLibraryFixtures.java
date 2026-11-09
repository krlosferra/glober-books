package com.globant.globerbooks.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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
}
