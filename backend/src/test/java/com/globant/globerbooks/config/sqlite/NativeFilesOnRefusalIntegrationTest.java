package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.support.FileFacts;
import com.globant.globerbooks.support.StoredLibraryFixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SEC-F-186 (Task 34b, item 7): when the stored library is refused, the files the driver extracted into the
 * private {@code tmp} directory are already narrowed to 0600 (they were left at the process umask, 0644,
 * on a refused start). Run under umask 022 in its own JVM, read before the JVM removes them.
 */
class NativeFilesOnRefusalIntegrationTest {

    @TempDir
    Path temp;

    @Test
    void extractedNativeFilesAreNarrowedWhenTheStartIsRefused() throws Exception {
        Path data = Files.createDirectory(temp.resolve("data"), FileFacts.dirAttr("rwx------"));
        Files.createDirectory(data.resolve("tmp"), FileFacts.dirAttr("rwx------"));
        Path file = StoredLibraryFixtures.library(data.resolve("library.db"), 2);
        StoredLibraryFixtures.run(file, false, "CREATE TABLE extra (name TEXT)");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder("sh", "-c", "umask 022 && exec \"$@\"", "sh", java, "-cp",
                System.getProperty("java.class.path"), "com.globant.globerbooks.config.sqlite.RefusedStartMain",
                file.toString()).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.waitFor()).isZero();
        List<String> modes = output.lines().filter(line -> line.startsWith("NATIVE ")).toList();
        System.out.println("REFUSED START native files: " + modes + " first line: " + output.lines().findFirst());
        assertThat(output).contains("REFUSED SCHEMA_DIFFERS");
        assertThat(modes).as("the driver extracted files").isNotEmpty().allMatch("NATIVE rw-------"::equals);
    }
}
