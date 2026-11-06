package com.globant.globerbooks.config.sqlite;

import static com.globant.globerbooks.support.TreeSnapshot.names;
import static com.globant.globerbooks.support.TreeSnapshot.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Guarded;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Reason;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Refusal;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.GuardChildMain;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Proves the data directory guard (Task 34a-1): creation modes independent of the umask, refusal of
 * an untrustworthy location with a fixed reason and nothing touched (US-009.3, US-009.6, SC-13,
 * SC-39, SC-11, SC-37, SEC-F-098, SEC-F-140). Expected values come from the task criteria.
 */
class DataDirectoryGuardTest {

    private static final String DB_NAME = "books.db";
    private static final String BOOK_DATA = "title=Dune;author=Herbert";

    @TempDir
    Path temp;

    private Path root;

    @BeforeEach
    void resolveRoot() throws IOException {
        root = temp.toRealPath();
    }

    @ParameterizedTest
    @ValueSource(strings = {"022", "000", "277", "077"})
    void createsDirectoryWith0700AndFileWith0600UnderAnyUmask(String umask) throws Exception {
        Path data = root.resolve("data");
        Path file = data.resolve(DB_NAME);

        Process child = new ProcessBuilder("sh", "-c", "umask " + umask + " && exec \"$0\" \"$@\"",
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), GuardChildMain.class.getName(), file.toString())
                .redirectErrorStream(true).start();
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = child.waitFor();

        System.out.println("UMASK " + umask + " exit=" + exit + " out=" + output.strip()
                + " dir=" + FileFacts.mode(data) + " file=" + FileFacts.mode(file)
                + " tmp=" + FileFacts.mode(data.resolve("tmp")));
        assertThat(exit).as(output).isZero();
        assertThat(FileFacts.mode(data)).isEqualTo("rwx------");
        assertThat(FileFacts.mode(file)).isEqualTo("rw-------");
        assertThat(FileFacts.mode(data.resolve("tmp"))).isEqualTo("rwx------");
        assertThat(names(data)).containsExactlyInAnyOrder(DB_NAME, "tmp");
    }

    @Test
    void createsOnlyTheMissingPartsAndLeavesNoOtherFile() throws Exception {
        Path file = root.resolve("data").resolve(DB_NAME);

        Guarded guarded = DataDirectoryGuard.check(file);

        assertThat(guarded.databaseFile()).isEqualTo(file);
        assertThat(guarded.nativeDirectory()).isEqualTo(root.resolve("data").resolve("tmp"));
        assertThat(Files.size(file)).isZero();
        assertThat(names(root)).containsExactly("data");
    }

    @Test
    void acceptsExistingOwnerOnlyDirectoryAndFileAndChangesNothing() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Path file = Files.writeString(data.resolve(DB_NAME), BOOK_DATA);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        Files.createDirectory(data.resolve("tmp"));
        Files.setPosixFilePermissions(data.resolve("tmp"), PosixFilePermissions.fromString("rwx------"));
        List<String> before = snapshot(root);

        DataDirectoryGuard.check(file);

        assertThat(snapshot(root)).isEqualTo(before);
    }

    @Test
    void acceptsAFileNarrowerThan0600() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Path file = Files.createFile(data.resolve(DB_NAME));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("r--------"));

        assertThat(DataDirectoryGuard.check(file).databaseFile()).isEqualTo(file);
    }

    @Test
    void refusesASymbolicLinkAsTheDataDirectory() throws Exception {
        Path real = directory(root.resolve("real"), "rwx------");
        Files.createSymbolicLink(root.resolve("data"), real);

        assertRefused(root.resolve("data").resolve(DB_NAME), Reason.SYMBOLIC_LINK);
    }

    @Test
    void refusesASymbolicLinkAsTheDatabaseFile() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Path target = Files.writeString(root.resolve("target.db"), BOOK_DATA);
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
        Files.createSymbolicLink(data.resolve(DB_NAME), target);

        assertRefused(data.resolve(DB_NAME), Reason.SYMBOLIC_LINK);
    }

    @Test
    void refusesADanglingSymbolicLinkAsTheDatabaseFileAndDoesNotCreateItsTarget() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Files.createSymbolicLink(data.resolve(DB_NAME), root.resolve("missing.db"));

        assertRefused(data.resolve(DB_NAME), Reason.SYMBOLIC_LINK);
        assertThat(Files.exists(root.resolve("missing.db"), LinkOption.NOFOLLOW_LINKS)).isFalse();
    }

    @Test
    void refusesASymbolicLinkInTheGuardedPath() throws Exception {
        Path real = directory(root.resolve("real"), "rwx------");
        directory(real.resolve("data"), "rwx------");
        Files.createSymbolicLink(root.resolve("alias"), real);

        assertRefused(root.resolve("alias").resolve("data").resolve(DB_NAME), Reason.SYMBOLIC_LINK);
    }

    @Test
    void refusesASymbolicLinkAsTheNativeDirectory() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Files.createSymbolicLink(data.resolve("tmp"), directory(root.resolve("elsewhere"), "rwx------"));

        assertRefused(data.resolve(DB_NAME), Reason.SYMBOLIC_LINK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rwxr-x---", "rwx--x---", "rwx---r--", "rwx-----x", "rwxrwxrwx", "rwx-w----"})
    void refusesAnyGroupOrOtherPermissionBitOnTheDirectory(String mode) throws Exception {
        Path data = directory(root.resolve("data"), mode);

        assertRefused(data.resolve(DB_NAME), Reason.PERMISSIONS_TOO_WIDE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rw-r-----", "rw--w----", "rw----r--", "rw-----w-", "rw-rw-rw-", "rwx--x--x"})
    void refusesAnyGroupOrOtherPermissionBitOnTheFile(String mode) throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Path file = Files.writeString(data.resolve(DB_NAME), BOOK_DATA);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(mode));

        assertRefused(file, Reason.PERMISSIONS_TOO_WIDE);
    }

    @Test
    void refusesANativeDirectoryWithAGroupBitAndDoesNotCreateTheFile() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        directory(data.resolve("tmp"), "rwxr-x---");

        assertRefused(data.resolve(DB_NAME), Reason.PERMISSIONS_TOO_WIDE);
        assertThat(names(data)).containsExactly("tmp");
    }

    @Test
    void refusesADirectoryOfAnotherOwner() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        otherOwner(data);

        assertRefused(data.resolve(DB_NAME), Reason.FOREIGN_OWNER);
    }

    @Test
    void refusesAFileOfAnotherOwner() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        Path file = Files.writeString(data.resolve(DB_NAME), BOOK_DATA);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        otherOwner(file);

        assertRefused(file, Reason.FOREIGN_OWNER);
    }

    @Test
    void refusesADirectoryInPlaceOfTheFileAndAFileInPlaceOfTheDirectory() throws Exception {
        Path data = directory(root.resolve("data"), "rwx------");
        directory(data.resolve(DB_NAME), "rwx------");
        Path blocker = Files.createFile(root.resolve("blocker"));
        Files.setPosixFilePermissions(blocker, PosixFilePermissions.fromString("rw-------"));

        assertRefused(data.resolve(DB_NAME), Reason.WRONG_FILE_TYPE);
        assertRefused(blocker.resolve(DB_NAME), Reason.WRONG_FILE_TYPE);
    }

    @Test
    void refusesAMissingParentOfTheDataDirectoryWithoutCreatingAnything() throws Exception {
        Path file = root.resolve("missing").resolve("data").resolve(DB_NAME);

        assertRefused(file, Reason.PARENT_UNUSABLE);
        assertThat(names(root)).isEmpty();
    }

    @Test
    void refusesAParentThatIsNotADirectory() throws Exception {
        Path blocker = Files.createFile(root.resolve("blocker"));

        assertRefused(blocker.resolve("data").resolve(DB_NAME), Reason.PARENT_UNUSABLE);
        assertThat(names(root)).containsExactly("blocker");
    }

    @Test
    void refusesARelativeANonNormalizedAndANullLocation() throws Exception {
        assertThat(reasonOf(Path.of("data", DB_NAME))).isEqualTo(Reason.LOCATION_NOT_ABSOLUTE);
        assertThat(reasonOf(root.resolve("a").resolve("..").resolve("data").resolve(DB_NAME)))
                .isEqualTo(Reason.LOCATION_NOT_ABSOLUTE);
        assertThat(reasonOf(Path.of("/" + DB_NAME))).isEqualTo(Reason.LOCATION_NOT_ABSOLUTE);
        assertThat(reasonOf(null)).isEqualTo(Reason.LOCATION_NOT_ABSOLUTE);
        assertThat(names(root)).isEmpty();
    }

    @Test
    void refusesWhenTheFileCannotBeCreatedAndLeavesNothingOutsideTheDataDirectory() throws Exception {
        Path tooLong = root.resolve("data").resolve("x".repeat(300));

        assertThat(refusal(tooLong).reason()).isEqualTo(Reason.CANNOT_CREATE);
        assertThat(names(root)).isEmpty();
    }

    @Test
    void usesOneFixedTextPerCategoryWithNoPathOwnerOrModeInIt() throws Exception {
        Path dataOne = directory(root.resolve("secret-one"), "rwxr-x---");
        Path dataTwo = directory(root.resolve("zeta-two"), "rwxrwxrwx");

        Refusal first = refusal(dataOne.resolve(DB_NAME));
        Refusal second = refusal(dataTwo.resolve(DB_NAME));

        assertThat(first.getMessage()).isEqualTo(second.getMessage())
                .isEqualTo(Reason.PERMISSIONS_TOO_WIDE.text())
                .doesNotContain("secret", "zeta", "/", "rwx", "root");
        assertThat(first).hasNoCause();
        for (Reason reason : Reason.values()) {
            assertThat(reason.text()).doesNotContain(root.toString(), System.getProperty("user.name"), "rwx");
        }
        assertThat(DataDirectoryGuard.check(root.resolve("data").resolve(DB_NAME)).toString())
                .doesNotContain(root.toString());
    }

    private void assertRefused(Path file, Reason expected) throws Exception {
        List<String> before = snapshot(root);

        Refusal refusal = refusal(file);

        assertThat(refusal.reason()).isEqualTo(expected);
        assertThat(refusal.getMessage()).isEqualTo(expected.text());
        assertThat(snapshot(root)).as("tree, modes and SHA-256 unchanged by the refusal").isEqualTo(before);
        System.out.println("REFUSED reason=" + refusal.reason() + " unchanged=true entries=" + before.size());
    }

    private static Refusal refusal(Path file) {
        try {
            DataDirectoryGuard.check(file);
        } catch (Refusal refusal) {
            return refusal;
        }
        throw new AssertionError("The guard accepted the location");
    }

    private static Reason reasonOf(Path file) {
        return refusal(file).reason();
    }

    private static Path directory(Path path, String mode) throws IOException {
        Files.createDirectory(path);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
        return path;
    }

    private static void otherOwner(Path path) throws IOException {
        try {
            Files.setOwner(path, FileSystems.getDefault().getUserPrincipalLookupService()
                    .lookupPrincipalByName("nobody"));
        } catch (IOException | UnsupportedOperationException failure) {
            Assumptions.abort("This environment cannot give a file to another user (needs root); the owner check "
                    + "is then not exercised here");
        }
    }
}
