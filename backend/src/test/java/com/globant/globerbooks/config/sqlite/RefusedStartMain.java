package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Child process of the SEC-F-186 proof: starts the provider on a stored library that is refused, and before
 * it exits prints the mode of every file the driver extracted into the private {@code tmp} directory. The
 * driver removes its files when the JVM ends, so the modes can only be read here. Argument: the database file.
 */
public final class RefusedStartMain {

    private RefusedStartMain() {
    }

    /**
     * Runs the child.
     *
     * @param args the database file
     * @throws IOException when the directory cannot be listed
     */
    public static void main(String[] args) throws IOException {
        Path file = Path.of(args[0]);
        try {
            SqliteDataSourceProvider.create(DataDirectoryGuard.check(file)).close();
            System.out.println("STARTED");
        } catch (StoredLibraryValidator.Refusal refusal) {
            System.out.println("REFUSED " + refusal.reason());
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(file.resolveSibling("tmp"))) {
            for (Path entry : entries) {
                System.out.println("NATIVE " + PosixFilePermissions.toString(Files.getPosixFilePermissions(entry)));
            }
        }
    }
}
