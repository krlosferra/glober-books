package com.globant.globerbooks.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** File-system facts used by the storage tests: modes, sidecars and hashes. */
public final class FileFacts {

    private FileFacts() {
    }

    public static List<String> sidecarsOf(Path dir, Path file) throws IOException {
        return regularFilesNextTo(dir, file).stream().map(path -> path.getFileName() + ":" + mode(path)).toList();
    }

    public static List<Path> regularFilesNextTo(Path dir, Path file) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isRegularFile).filter(path -> !path.equals(file)).toList();
        }
    }

    public static String mode(Path path) {
        try {
            return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    public static boolean isNotWiderThan0600(Path path) {
        Set<PosixFilePermission> allowed = PosixFilePermissions.fromString("rw-------");
        try {
            return allowed.containsAll(Files.getPosixFilePermissions(path));
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    public static FileAttribute<Set<PosixFilePermission>> dirAttr(String text) {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(text));
    }

    public static FileAttribute<Set<PosixFilePermission>> fileAttr(String text) {
        return dirAttr(text);
    }

    public static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
