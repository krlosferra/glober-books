package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Reason;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Refusal;
import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.TreeSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Task 34a-2 follow-ups of the guard: SEC-F-182 (hard links) and SEC-F-184 (a partial creation). */
class DataDirectoryGuardCleanupTest {

    @TempDir
    Path root;

    @Test
    void refusesADatabaseFileWithMoreThanOneHardLinkAndChangesNothing() throws Exception {
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        Path file = Files.createFile(data.resolve("library.db"), FileFacts.fileAttr("rw-------"));
        Files.createLink(root.resolve("other-name"), file);
        List<String> before = TreeSnapshot.snapshot(root);

        assertThatThrownBy(() -> DataDirectoryGuard.check(file)).isInstanceOf(Refusal.class)
                .extracting(failure -> ((Refusal) failure).reason()).isEqualTo(Reason.MULTIPLE_LINKS);
        assertThat(TreeSnapshot.snapshot(root)).isEqualTo(before);
    }

    @Test
    void failedCreationRemovesOnlyWhatTheCallCreated() throws Exception {
        Path data = Files.createDirectory(root.resolve("data"), FileFacts.dirAttr("rwx------"));
        Path keep = Files.writeString(data.resolve("keep"), "mine");
        Path tooLong = data.resolve("x".repeat(300));

        assertThatThrownBy(() -> DataDirectoryGuard.check(tooLong)).isInstanceOf(Refusal.class)
                .extracting(failure -> ((Refusal) failure).reason()).isEqualTo(Reason.CANNOT_CREATE);
        assertThat(TreeSnapshot.names(data)).containsExactly("keep");
        assertThat(keep).hasContent("mine");
        assertThat(TreeSnapshot.names(root)).containsExactly("data");
    }

    @Test
    void failedCreationOfANewDirectoryLeavesNothing() throws Exception {
        Path tooLong = root.resolve("data").resolve("x".repeat(300));

        assertThatThrownBy(() -> DataDirectoryGuard.check(tooLong)).isInstanceOf(Refusal.class);
        assertThat(TreeSnapshot.names(root)).isEmpty();
    }
}
