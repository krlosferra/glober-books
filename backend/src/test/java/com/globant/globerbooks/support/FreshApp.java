package com.globant.globerbooks.support;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts the packaged jar on an empty library in a temporary location that satisfies the data directory
 * guard (directory 0700, database file 0600), the same way the durability tests do (Task 49+50).
 */
public final class FreshApp {

    private FreshApp() {
    }

    /**
     * Creates the guarded location below the given directory and starts the packaged jar on it.
     *
     * @param parent an existing directory the test owns
     * @return the running application
     * @throws Exception when the location cannot be created or the process does not become ready
     */
    public static AppProcess start(Path parent) throws Exception {
        Path data = Files.createDirectory(parent.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createFile(data.resolve("library.db"), FileFacts.fileAttr("rw-------"));
        return AppProcess.start(data, Files.createTempFile(parent, "console", ".log"));
    }

    /**
     * Stops the application and asserts that no process of it is left.
     *
     * @param app the application, or null when it never started
     * @throws InterruptedException when the wait is interrupted
     */
    public static void stopAndCheck(AppProcess app) throws InterruptedException {
        if (app == null) {
            return;
        }
        app.stop();
        if (app.isAlive()) {
            throw new IllegalStateException("The application process is still alive after stop");
        }
    }
}
