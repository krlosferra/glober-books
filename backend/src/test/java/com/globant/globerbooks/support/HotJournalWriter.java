package com.globant.globerbooks.support;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Produces a genuine hot journal (R-20, SEC-F-074): a separate JVM ({@code JournalWriterMain}) opens the
 * database with the rollback journal, starts a transaction that adds many books with a tiny page cache so
 * that dirty pages are written into the main file (which grows) after the journal is synced, prints
 * {@code READY} and waits; this class then kills it with SIGKILL, so the transaction is never committed
 * nor rolled back and {@code <file>-journal} stays beside the file. The journal is SQLite's own.
 */
public final class HotJournalWriter {

    private static final int WAIT_SECONDS = 60;

    private HotJournalWriter() {
    }

    /**
     * Leaves a hot journal beside the file.
     *
     * @param file the database file (must hold the Book table)
     * @throws Exception when the child cannot be started, never says READY, or leaves no journal
     */
    public static void leaveHotJournal(Path file) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                "com.globant.globerbooks.config.sqlite.JournalWriterMain", file.toString())
                .redirectErrorStream(true).start();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            CompletableFuture<Boolean> ready = CompletableFuture.supplyAsync(() -> awaitReady(reader));
            if (!ready.get(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The writer JVM ended before READY");
            }
        } finally {
            process.destroyForcibly().waitFor(WAIT_SECONDS, TimeUnit.SECONDS);
        }
        if (!Files.isRegularFile(journalOf(file)) || Files.size(journalOf(file)) == 0) {
            throw new IllegalStateException("The killed writer left no hot journal");
        }
    }

    private static boolean awaitReady(BufferedReader reader) {
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if ("READY".equals(line)) {
                    return true;
                }
            }
            return false;
        } catch (java.io.IOException failure) {
            return false;
        }
    }

    /**
     * Returns the journal path of a database file.
     *
     * @param file the database file
     * @return the file with {@code -journal} appended
     */
    public static Path journalOf(Path file) {
        return file.resolveSibling(file.getFileName() + "-journal");
    }

    /**
     * Computes the file as SQLite's own rollback leaves it, on copies in another directory, so that the
     * original bytes stay untouched: the main file and its journal are copied, a plain read-only reference
     * connection opens the copy (SQLite rolls the journal back there), and the copy is hashed.
     *
     * @param file the database file with its hot journal
     * @param scratch an existing empty directory for the copies
     * @return the SHA-256 of the main file after the rollback
     * @throws Exception when the reference rollback fails
     */
    public static String hashAfterReferenceRollback(Path file, Path scratch) throws Exception {
        Path copy = scratch.resolve(file.getFileName());
        Files.copy(file, copy, StandardCopyOption.COPY_ATTRIBUTES);
        Files.copy(journalOf(file), journalOf(copy), StandardCopyOption.COPY_ATTRIBUTES);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + copy);
                Statement statement = connection.createStatement()) {
            statement.execute("SELECT count(*) FROM sqlite_master");
        }
        if (Files.exists(journalOf(copy))) {
            throw new IllegalStateException("The reference rollback did not consume the journal");
        }
        return FileFacts.sha256(copy);
    }
}
