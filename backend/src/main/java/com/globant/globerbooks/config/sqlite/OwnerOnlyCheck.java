package com.globant.globerbooks.config.sqlite;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 * Owner, mode and link-count checks of an existing path for {@link DataDirectoryGuard}. Reads
 * attributes only; every refusal carries a fixed reason.
 *
 * <p>Limit: the owner is compared by name, as the platform lookup service gives it; the numeric
 * user id is not compared because the standard library has no portable way to read the id of the
 * running process.
 */
final class OwnerOnlyCheck {

    private static final String LINK_COUNT = "unix:nlink";
    private static final Set<PosixFilePermission> WIDER_THAN_OWNER = Set.of(
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE);

    private OwnerOnlyCheck() {
    }

    /**
     * Refuses a path of another owner, with a group or other permission, or (for a file) with more
     * than one hard link.
     *
     * @param path an existing directory or regular file
     * @param directory whether the path is the directory kind
     * @throws IOException when the attributes cannot be read
     */
    static void require(Path path, boolean directory) throws IOException {
        PosixFileAttributes attributes = Files.readAttributes(path, PosixFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        UserPrincipal user = FileSystems.getDefault().getUserPrincipalLookupService()
                .lookupPrincipalByName(System.getProperty("user.name"));
        if (!attributes.owner().equals(user)) {
            throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.FOREIGN_OWNER);
        }
        for (PosixFilePermission permission : attributes.permissions()) {
            if (WIDER_THAN_OWNER.contains(permission)) {
                throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.PERMISSIONS_TOO_WIDE);
            }
        }
        if (!directory && linkCount(path) > 1) {
            throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.MULTIPLE_LINKS);
        }
    }

    /** Returns the hard-link count, or 1 where the platform does not report it. */
    private static int linkCount(Path path) throws IOException {
        try {
            return (Integer) Files.getAttribute(path, LINK_COUNT, LinkOption.NOFOLLOW_LINKS);
        } catch (UnsupportedOperationException | IllegalArgumentException unsupported) {
            return 1;
        }
    }
}
