package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Writer process of the hot-journal proof (started by {@code HotJournalWriter}, killed by the test). It adds
 * many books in one uncommitted transaction with a tiny page cache, so dirty pages reach the main file, then
 * prints {@code READY} and waits for the kill. Argument: the database file.
 */
public final class JournalWriterMain {

    private static final int ROWS = 3000;
    private static final int CACHE_PAGES = 5;
    private static final int TITLE_LENGTH = 400;

    private JournalWriterMain() {
    }

    /**
     * Runs the writer.
     *
     * @param args the database file
     * @throws SQLException when the transaction cannot be written
     * @throws IOException when standard input cannot be read
     */
    public static void main(String[] args) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + args[0])) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA synchronous = FULL");
                statement.execute("PRAGMA cache_size = " + CACHE_PAGES);
            }
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO books (id, title, author, "
                    + "status, rating, date_added, date_completed) VALUES (?, ?, 'Writer', 'unread', NULL, "
                    + "'2026-01-02T03:04:05.678Z', NULL)")) {
                for (int index = 0; index < ROWS; index++) {
                    insert.setString(1, String.format("ffffffff-0000-4000-8000-%012x", index));
                    insert.setString(2, "w".repeat(TITLE_LENGTH) + index);
                    insert.executeUpdate();
                }
            }
            System.out.println("READY");
            System.out.flush();
            System.in.read();
        }
    }
}
