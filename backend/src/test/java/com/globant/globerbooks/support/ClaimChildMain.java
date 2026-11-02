package com.globant.globerbooks.support;

import com.globant.globerbooks.config.sqlite.SqliteDataSourceProvider;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Second process of the claim proof (plan AP-12.2). Started by {@code SqliteStoreIntegrationTest}
 * in its own JVM. Arguments: a mode and the database file.
 *
 * <ul>
 *   <li>{@code hold}: starts the provider (claim), prints {@code READY}, then waits until standard
 *       input delivers a line or ends, closes the pool and exits with 0.
 *   <li>{@code try}: starts the provider; on success prints {@code CLAIMED} and exits with 0; on
 *       failure prints {@code FAILED} followed by the messages of the cause chain and exits with 2.
 * </ul>
 */
public final class ClaimChildMain {

    /** Exit code of the {@code try} mode when the claim is refused. */
    public static final int EXIT_REFUSED = 2;
    /** Exit code for a wrong command line. */
    public static final int EXIT_USAGE = 64;

    private ClaimChildMain() {
    }

    /**
     * Runs the child.
     *
     * @param args mode ({@code hold} or {@code try}) and database file
     * @throws IOException when standard input cannot be read
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.exit(EXIT_USAGE);
        }
        Path file = Path.of(args[1]);
        try (HikariDataSource pool = SqliteDataSourceProvider.create(file)) {
            if (!pool.isRunning()) {
                System.exit(EXIT_REFUSED);
            }
            if ("hold".equals(args[0])) {
                System.out.println("READY");
                System.out.flush();
                System.in.read();
            } else {
                System.out.println("CLAIMED");
            }
        } catch (IllegalStateException failure) {
            System.out.println("FAILED " + chain(failure));
            System.exit(EXIT_REFUSED);
        }
    }

    private static String chain(Throwable failure) {
        StringBuilder text = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            text.append(current.getMessage()).append(" | ");
        }
        return text.toString();
    }
}
