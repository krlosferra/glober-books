package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Narrows the files the SQLite driver extracts into the private native-library directory (the
 * library and its lock file are written with the process umask, for example 0644). The directory
 * itself is 0700, so nobody else could reach them; this makes every regular file there 0600 as
 * well (SC-13, SEC-F-181). Called after the library is loaded, when execute permission is no
 * longer needed. Only regular files are touched, links are never followed, and a failure is
 * ignored because the directory protection stays in force.
 */
final class NativeDirectoryModes {

    private static final String FILE_MODE = "rw-------";

    private NativeDirectoryModes() {
    }

    /**
     * Sets mode 0600 on every regular file directly inside the directory.
     *
     * @param nativeDirectory the private native-library directory
     */
    static void narrow(Path nativeDirectory) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(nativeDirectory)) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    Files.setPosixFilePermissions(entry, PosixFilePermissions.fromString(FILE_MODE));
                }
            }
        } catch (IOException | RuntimeException failure) {
            return;
        }
    }
}
