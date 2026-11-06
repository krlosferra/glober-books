package com.globant.globerbooks.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Forces a failure inside a write by a trigger created on the test database only (plan: fault injection
 * by a trigger; nothing in production code). The trigger fires after the row change of an update of the
 * status, with {@code RAISE(FAIL)}: the statement stops with the row already changed in the open
 * transaction, so only the rollback of the whole transaction can leave the book as it was.
 *
 * <p>The connection comes from the application's own pool of one connection and is returned at once, so
 * the pool is never held by the helper.
 */
public final class FaultInjection {

    /** Message text inside the trigger; the tests assert that it never reaches a client. */
    public static final String SECRET_TEXT = "forced-fault-secret-marker";

    private static final String TRIGGER = "fault_after_status_change";

    private FaultInjection() {
    }

    /**
     * Creates the trigger on the test database.
     *
     * @param dataSource the data source of the application under test
     * @throws SQLException when the statement fails
     */
    public static void armAfterStatusChange(DataSource dataSource) throws SQLException {
        run(dataSource, "CREATE TRIGGER " + TRIGGER + " AFTER UPDATE OF status ON books BEGIN SELECT RAISE(FAIL, '"
                + SECRET_TEXT + "'); END");
    }

    /**
     * Removes the trigger.
     *
     * @param dataSource the data source of the application under test
     * @throws SQLException when the statement fails
     */
    public static void disarm(DataSource dataSource) throws SQLException {
        run(dataSource, "DROP TRIGGER IF EXISTS " + TRIGGER);
    }

    private static void run(DataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
