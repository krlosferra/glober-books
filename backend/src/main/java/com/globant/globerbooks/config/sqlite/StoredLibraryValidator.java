package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates an existing database file before anything writes to it (Task 34b, US-009.5, DEC-028,
 * SC-32, SEC-F-068, SEC-F-074). The file header is read first with plain I/O, before the pool opens;
 * then, on the claimed connection with {@code query_only} on, the structure, {@code quick_check} and
 * the rows are judged in that order. Every refusal is a {@link Refusal} with one fixed reason per
 * category: it never holds a cell value, a path, an id or the text of a SQLite message.
 */
public final class StoredLibraryValidator {

    private static final String SCHEMA_RESOURCE = "schema.sql";
    private static final int MAX_BOOKS = 5000;
    private static final int ID_BYTES = 36;
    private static final int DATE_BYTES = 24;
    private static final int MAX_TITLE_BYTES = 2000;
    private static final int MAX_AUTHOR_BYTES = 800;
    private static final int MAX_STATUS_BYTES = 9;
    private static final int MAX_TITLE = 500;
    private static final int MAX_AUTHOR = 200;
    private static final Set<String> STATUSES = Set.of("unread", "reading", "completed");
    private static final Pattern ID_FORMAT = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern DATE_FORMAT = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    private static final DateTimeFormatter STRICT_TIMESTAMP = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withResolverStyle(ResolverStyle.STRICT);
    private static final String SQL_OBJECTS = "SELECT name, type, tbl_name, sql FROM sqlite_master";
    private static final String SQL_QUICK_CHECK = "PRAGMA quick_check";
    private static final String SQL_PRECHECK = "SELECT typeof(id), typeof(title), typeof(author), typeof(status), "
            + "typeof(rating), typeof(date_added), typeof(date_completed), length(CAST(id AS BLOB)), "
            + "length(CAST(title AS BLOB)), length(CAST(author AS BLOB)), length(CAST(status AS BLOB)), "
            + "length(CAST(date_added AS BLOB)), length(CAST(date_completed AS BLOB)) FROM books "
            + "ORDER BY rowid LIMIT " + (MAX_BOOKS + 1);
    private static final String SQL_ROWS = "SELECT id, title, author, status, rating, date_added, date_completed "
            + "FROM books ORDER BY rowid LIMIT " + (MAX_BOOKS + 1);

    private StoredLibraryValidator() {
    }

    /** The category of a refusal. Each value has one fixed text. */
    public enum Reason {
        /** The file is not a database of the kind used by the application. */
        NOT_A_DATABASE("Stored library: the file is not a database of the kind used by the application"),
        /** The header marks the file as using write-ahead logging. */
        WAL_MARKED("Stored library: the file is marked as using write-ahead logging"),
        /** The file is damaged. */
        DAMAGED("Stored library: the database file is damaged"),
        /** The database holds no table. */
        NO_TABLES("Stored library: the database holds no table"),
        /** The tables, indexes or the definition of the table differ from the Book structure. */
        SCHEMA_DIFFERS("Stored library: the structure differs from the Book structure"),
        /** A view, trigger or virtual table exists. */
        FORBIDDEN_OBJECT("Stored library: a view, trigger or virtual table exists"),
        /** More than 5,000 books. */
        TOO_MANY_BOOKS("Stored library: there are too many books"),
        /** A stored value is larger than any valid value. */
        OVERSIZED_CELL("Stored library: a stored value is larger than allowed"),
        /** A row violates a format, a limit or an invariant. */
        INVALID_ROW("Stored library: a book violates a format, a limit or an invariant");

        private final String text;

        Reason(String text) {
            this.text = text;
        }

        /**
         * Returns the fixed text of this reason.
         *
         * @return the text, identical on every use
         */
        public String text() {
            return text;
        }
    }

    /** Refusal of the validator; the message is the fixed text of the reason and there is no cause. */
    public static final class Refusal extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        private final Reason reason;

        Refusal(Reason reason) {
            super(reason.text());
            this.reason = reason;
        }

        /**
         * Returns the category of the refusal.
         *
         * @return the reason
         */
        public Reason reason() {
            return reason;
        }
    }

    /**
     * Judges the stored library on the claimed connection. The caller has put the connection in
     * {@code query_only} mode and skips this call for a zero-length file.
     *
     * @param connection the claimed connection
     * @throws Refusal with the category of the first failure
     */
    public static void validate(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            requireStructure(statement);
            requireQuickCheck(statement);
            requireRowBounds(statement);
            requireRows(statement);
        } catch (SQLException failure) {
            throw new Refusal(Reason.DAMAGED);
        }
    }

    private static void requireStructure(Statement statement) throws SQLException {
        int tables = 0;
        String booksSql = null;
        boolean otherObject = false;
        try (ResultSet result = statement.executeQuery(SQL_OBJECTS)) {
            while (result.next()) {
                String type = result.getString(2);
                String sql = result.getString(4);
                if ("view".equals(type) || "trigger".equals(type)
                        || (sql != null && sql.stripLeading().toUpperCase(Locale.ROOT)
                                .startsWith("CREATE VIRTUAL"))) {
                    throw new Refusal(Reason.FORBIDDEN_OBJECT);
                }
                if ("table".equals(type) && "books".equals(result.getString(1))) {
                    tables++;
                    booksSql = sql;
                } else if (!isKeyIndex(type, result.getString(1), result.getString(3), sql)) {
                    otherObject = true;
                }
            }
        }
        if (tables == 0 && !otherObject) {
            throw new Refusal(Reason.NO_TABLES);
        }
        if (tables != 1 || otherObject || !normalise(loadSchema()).equals(normalise(booksSql))) {
            throw new Refusal(Reason.SCHEMA_DIFFERS);
        }
    }

    private static boolean isKeyIndex(String type, String name, String table, String sql) {
        return "index".equals(type) && sql == null && "books".equals(table)
                && "sqlite_autoindex_books_1".equals(name);
    }

    private static String loadSchema() {
        try (InputStream stream = StoredLibraryValidator.class.getClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) {
            return new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    static String normalise(String ddl) {
        return ddl.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("--[^\\n]*", " ")
                .replace("IF NOT EXISTS ", "").replaceAll("\\s+", " ").replaceAll("\\s*;\\s*$", "").trim();
    }

    private static void requireQuickCheck(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery(SQL_QUICK_CHECK)) {
            if (!result.next() || !"ok".equals(result.getString(1)) || result.next()) {
                throw new Refusal(Reason.DAMAGED);
            }
        }
    }

    private static void requireRowBounds(Statement statement) throws SQLException {
        int count = 0;
        try (ResultSet result = statement.executeQuery(SQL_PRECHECK)) {
            while (result.next()) {
                if (++count > MAX_BOOKS) {
                    throw new Refusal(Reason.TOO_MANY_BOOKS);
                }
                requireTypes(result);
                requireByteBounds(result);
            }
        }
    }

    private static void requireTypes(ResultSet result) throws SQLException {
        boolean textColumns = true;
        for (int column = 1; column <= 4; column++) {
            textColumns &= "text".equals(result.getString(column));
        }
        textColumns &= "text".equals(result.getString(6));
        String rating = result.getString(5);
        String completed = result.getString(7);
        if (!textColumns || !("null".equals(rating) || "integer".equals(rating))
                || !("null".equals(completed) || "text".equals(completed))) {
            throw new Refusal(Reason.INVALID_ROW);
        }
    }

    private static void requireByteBounds(ResultSet result) throws SQLException {
        if (result.getLong(8) > ID_BYTES || result.getLong(9) > MAX_TITLE_BYTES
                || result.getLong(10) > MAX_AUTHOR_BYTES || result.getLong(11) > MAX_STATUS_BYTES
                || result.getLong(12) > DATE_BYTES || result.getLong(13) > DATE_BYTES) {
            throw new Refusal(Reason.OVERSIZED_CELL);
        }
    }

    private static void requireRows(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery(SQL_ROWS)) {
            while (result.next()) {
                Integer rating = result.getObject(5) == null ? null : result.getInt(5);
                requireRow(result.getString(1), result.getString(2), result.getString(3), result.getString(4),
                        rating, result.getString(6), result.getString(7));
            }
        }
    }

    /**
     * Judges one stored row: formats, limits (title and author in code points) and INV-1 to INV-4.
     *
     * @param id the stored id
     * @param title the stored title
     * @param author the stored author
     * @param status the stored status
     * @param rating the stored rating, or null
     * @param added the stored creation timestamp
     * @param completed the stored completion timestamp, or null
     * @throws Refusal with {@link Reason#INVALID_ROW} when the row is not valid
     */
    static void requireRow(String id, String title, String author, String status, Integer rating, String added,
            String completed) {
        boolean valid = ID_FORMAT.matcher(id).matches()
                && withinCodePoints(title, MAX_TITLE) && withinCodePoints(author, MAX_AUTHOR)
                && STATUSES.contains(status) && isTimestamp(added)
                && (rating == null || (rating >= 1 && rating <= 5 && "completed".equals(status)))
                && "completed".equals(status) == (completed != null)
                && (completed == null || isTimestamp(completed));
        if (!valid) {
            throw new Refusal(Reason.INVALID_ROW);
        }
    }

    private static boolean withinCodePoints(String text, int maximum) {
        int count = text.codePointCount(0, text.length());
        return count >= 1 && count <= maximum && text.indexOf('\0') < 0;
    }

    private static boolean isTimestamp(String text) {
        if (!DATE_FORMAT.matcher(text).matches()) {
            return false;
        }
        try {
            LocalDateTime.parse(text, STRICT_TIMESTAMP);
            return true;
        } catch (DateTimeParseException failure) {
            return false;
        }
    }
}
