package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Objects;
import java.util.Set;

/**
 * Prepares and vets the place where the database lives, before any connection is opened.
 *
 * <p>Layout: {@code <data directory>/<database file>} and the private native-library directory
 * {@code <data directory>/tmp}. Missing parts are created with exact modes (directories 0700, file
 * 0600), set explicitly after creation so that the process umask cannot leave them wider or
 * narrower. Existing parts are never changed: on a POSIX file system an existing directory or file
 * is refused when it is a symbolic link, is not of the expected type, belongs to another user, or
 * has any group or other permission bit. A symbolic link anywhere in the path is refused, as is a
 * path that is not absolute and normalized, a missing parent of the data directory, and anything
 * that cannot be created. The data directory is created one level deep only.
 *
 * <p>All inspections happen before the first creation, and they read attributes only (never the
 * content of the file), so a refusal creates, changes and deletes nothing. A refusal carries a fixed
 * {@link Reason}; the text never holds a path, an owner, a mode or book data.
 *
 * <p>Documented limits: (1) On a file system without POSIX attributes the owner and mode checks are
 * not made (US-009.6 platform limit); the symbolic link, type and identity checks still are. (2)
 * Between the check and the open of the file by the database driver a local attacker who can write
 * to the parent directory could swap the file. The identity remembered in {@link Guarded} narrows
 * this window by comparing device and inode after the pool opens, but it cannot close it, and the
 * remaining race cannot be tested without a test-only switch (A-09). The data directory is mode
 * 0700 and owned by the user, which removes the attacker except the same user or root.
 */
public final class DataDirectoryGuard {

    private static final String NATIVE_DIRECTORY = "tmp";
    private static final String DIRECTORY_MODE = "rwx------";
    private static final String FILE_MODE = "rw-------";
    private static final String POSIX_VIEW = "posix";
    private static final Set<PosixFilePermission> WIDER_THAN_OWNER = Set.of(
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE);

    private DataDirectoryGuard() {
    }

    /**
     * Checks the location, creates what is missing and remembers the identity of the file.
     *
     * @param databaseFile the absolute, normalized path of the database file
     * @return the vetted location with the identity of the file
     * @throws Refusal with a fixed reason when the location is not trustworthy or cannot be created
     */
    public static Guarded check(Path databaseFile) {
        Path file = requireAbsolute(databaseFile);
        Path directory = Objects.requireNonNull(file.getParent());
        Path nativeDirectory = directory.resolve(NATIVE_DIRECTORY);
        rejectSymbolicLinks(file);
        rejectSymbolicLinks(nativeDirectory);
        requireDirectory(directory.getParent(), Reason.PARENT_UNUSABLE);
        boolean posix = file.getFileSystem().supportedFileAttributeViews().contains(POSIX_VIEW);
        boolean directoryExists = inspect(directory, true, posix);
        boolean nativeExists = inspect(nativeDirectory, true, posix);
        boolean fileExists = inspect(file, false, posix);
        try {
            if (!directoryExists) {
                createDirectory(directory, posix);
            }
            if (!nativeExists) {
                createDirectory(nativeDirectory, posix);
            }
            if (!fileExists) {
                createFile(file, posix);
            }
            return new Guarded(file, nativeDirectory, FileIdentity.of(file));
        } catch (IOException | RuntimeException failure) {
            throw new Refusal(Reason.CANNOT_CREATE);
        }
    }

    private static Path requireAbsolute(Path databaseFile) {
        if (databaseFile == null || !databaseFile.isAbsolute() || !databaseFile.equals(databaseFile.normalize())
                || databaseFile.getNameCount() < 2) {
            throw new Refusal(Reason.LOCATION_NOT_ABSOLUTE);
        }
        return databaseFile;
    }

    private static void rejectSymbolicLinks(Path path) {
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new Refusal(Reason.SYMBOLIC_LINK);
            }
        }
    }

    private static void requireDirectory(Path path, Reason reason) {
        if (path == null || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new Refusal(reason);
        }
    }

    /** Returns whether the path exists; refuses when it exists and is not trustworthy. */
    private static boolean inspect(Path path, boolean expectDirectory, boolean posix) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try {
            BasicFileAttributes basic = Files.readAttributes(path, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (basic.isSymbolicLink()) {
                throw new Refusal(Reason.SYMBOLIC_LINK);
            }
            if (expectDirectory ? !basic.isDirectory() : !basic.isRegularFile()) {
                throw new Refusal(Reason.WRONG_FILE_TYPE);
            }
            if (posix) {
                requireOwnerOnly(Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
            }
            return true;
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof Refusal refusal) {
                throw refusal;
            }
            throw new Refusal(Reason.CANNOT_CREATE);
        }
    }

    private static void requireOwnerOnly(PosixFileAttributes attributes) throws IOException {
        if (!attributes.owner().equals(currentUser())) {
            throw new Refusal(Reason.FOREIGN_OWNER);
        }
        for (PosixFilePermission permission : attributes.permissions()) {
            if (WIDER_THAN_OWNER.contains(permission)) {
                throw new Refusal(Reason.PERMISSIONS_TOO_WIDE);
            }
        }
    }

    private static UserPrincipal currentUser() throws IOException {
        return FileSystems.getDefault().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
    }

    private static void createDirectory(Path directory, boolean posix) throws IOException {
        if (posix) {
            Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString(DIRECTORY_MODE)));
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString(DIRECTORY_MODE));
        } else {
            Files.createDirectory(directory);
        }
    }

    private static void createFile(Path file, boolean posix) throws IOException {
        if (posix) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString(FILE_MODE)));
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(FILE_MODE));
        } else {
            Files.createFile(file);
        }
    }

    /** Why the guard refused. Each value has one fixed text; no path, owner or mode is ever added. */
    public enum Reason {
        /** The path is null, relative, not normalized or has no directory part. */
        LOCATION_NOT_ABSOLUTE("The data location is not an absolute, normalized path"),
        /** The data directory, the database file or a part of their path is a symbolic link. */
        SYMBOLIC_LINK("The data location contains a symbolic link"),
        /** The parent of the data directory is missing or is not a directory. */
        PARENT_UNUSABLE("The parent of the data directory is missing or is not a directory"),
        /** The data directory or the database file is not of the expected type. */
        WRONG_FILE_TYPE("The data directory or the database file is not of the expected type"),
        /** The data directory or the database file belongs to another user. */
        FOREIGN_OWNER("The data directory or the database file belongs to another user"),
        /** The data directory or the database file has a group or other permission. */
        PERMISSIONS_TOO_WIDE("The data directory or the database file is accessible to group or others"),
        /** The directory or the file could not be created or inspected. */
        CANNOT_CREATE("The data directory or the database file could not be created or opened"),
        /** The database file is not the file that was checked. */
        IDENTITY_CHANGED("The database file is not the file that was checked");

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

    /** Refusal of the guard; the message is the fixed text of the reason and there is no cause. */
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
     * Device and inode of a file (through {@code fileKey}, null where the platform has none) and
     * whether it is a regular file, read without following links.
     *
     * @param fileKey the platform file key, or null
     * @param regularFile whether the path is a regular file and not a link
     */
    public record FileIdentity(Object fileKey, boolean regularFile) {

        /**
         * Reads the identity of a path without following a symbolic link.
         *
         * @param path the file
         * @return its identity
         * @throws IOException when the attributes cannot be read
         */
        public static FileIdentity of(Path path) throws IOException {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            return new FileIdentity(attributes.fileKey(), attributes.isRegularFile());
        }
    }

    /**
     * The vetted location. The text form holds no path.
     *
     * @param databaseFile the database file
     * @param nativeDirectory the private native-library directory
     * @param identity the identity of the file when it was vetted
     */
    public record Guarded(Path databaseFile, Path nativeDirectory, FileIdentity identity) {

        /**
         * Compares the file now at the path with the file that was vetted; to be called after the
         * pool opened. Reads attributes only.
         *
         * @throws Refusal with {@link Reason#IDENTITY_CHANGED} when the file is another file, is not
         *     a regular file, or is gone
         */
        public void requireSameFile() {
            try {
                if (!identity.regularFile() || !identity.equals(FileIdentity.of(databaseFile))) {
                    throw new Refusal(Reason.IDENTITY_CHANGED);
                }
            } catch (IOException failure) {
                throw new Refusal(Reason.IDENTITY_CHANGED);
            }
        }

        @Override
        public String toString() {
            return "Guarded";
        }
    }
}
