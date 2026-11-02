package com.globant.globerbooks.support;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sqlite.SQLiteConnection;

/** Reads settings and connection identity from the pool under test. */
public final class PoolFacts {

    private static final int WAIT_SECONDS = 60;

    private PoolFacts() {
    }

    public static Map<String, String> settings(HikariDataSource pool) throws SQLException {
        Map<String, String> values = new LinkedHashMap<>();
        try (Connection connection = pool.getConnection(); Statement statement = connection.createStatement()) {
            for (String name : List.of("journal_mode", "synchronous", "locking_mode", "foreign_keys",
                    "busy_timeout", "trusted_schema")) {
                try (ResultSet result = statement.executeQuery("PRAGMA " + name)) {
                    values.put(name, result.next() ? result.getString(1).toLowerCase(Locale.ROOT) : "");
                }
            }
        }
        return values;
    }

    public static SQLiteConnection rawConnection(HikariDataSource pool) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            return connection.unwrap(SQLiteConnection.class);
        }
    }

    public static SQLiteConnection awaitNewConnection(HikariDataSource pool, SQLiteConnection old)
            throws SQLException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            SQLiteConnection current = rawConnection(pool);
            if (current != old) {
                return current;
            }
            Thread.onSpinWait();
        }
        throw new IllegalStateException("The pool did not replace the evicted connection");
    }

    public static String storedDdl(HikariDataSource pool) throws SQLException {
        try (Connection connection = pool.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT sql FROM sqlite_master WHERE name = 'books'")) {
            return result.next() ? result.getString(1) : "";
        }
    }
}
