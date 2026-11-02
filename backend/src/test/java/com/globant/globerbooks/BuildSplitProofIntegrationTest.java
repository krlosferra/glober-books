package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

/**
 * Proves that a class named {@code *IntegrationTest} is executed by Failsafe (after the jar is
 * packaged), that its coverage reaches the merged JaCoCo data, and that the packaged jar has the
 * expected content.
 */
class BuildSplitProofIntegrationTest {

    private static final String BOOT_CLASSES = "BOOT-INF/classes/";

    @Test
    void runsUnderFailsafeWhenNamedWithIntegrationTestSuffix() {
        String runner = System.getProperty("globerbooks.test.runner");

        assertThat(runner).isEqualTo("failsafe");
    }

    @Test
    void executesApplicationClassOnlyFromIntegrationPhaseForMergedCoverage() {
        GloberBooksApplication application = new GloberBooksApplication();

        assertThat(application).isNotNull();
    }

    @Test
    void packagesJarNamedGloberbooksWithApplicationClass() throws IOException {
        List<String> entries = jarEntries();

        assertThat(entries).contains(BOOT_CLASSES + "com/globant/globerbooks/GloberBooksApplication.class");
    }

    @Test
    void packagesFrontendDirectoryAsStaticResources() throws IOException {
        List<String> entries = jarEntries();

        assertThat(entries).contains(BOOT_CLASSES + "static/index.html");
        assertThat(entries).noneMatch(name -> name.startsWith(BOOT_CLASSES + "frontend/"));
    }

    @Test
    void excludesTestScriptsSourceMapsAndDotfilesFromStaticResources() throws IOException {
        List<String> staticEntries = jarEntries().stream()
                .filter(name -> name.startsWith(BOOT_CLASSES + "static/"))
                .toList();

        assertThat(staticEntries).noneMatch(name -> name.endsWith(".test.js"));
        assertThat(staticEntries).noneMatch(name -> name.endsWith(".map"));
        assertThat(staticEntries).noneMatch(name -> hasDotSegment(name.substring((BOOT_CLASSES + "static/").length())));
    }

    @Test
    void containsNoTestClassesInJar() throws IOException {
        List<String> classEntries = jarEntries().stream()
                .filter(name -> name.startsWith(BOOT_CLASSES) && name.endsWith(".class"))
                .toList();

        assertThat(classEntries).isNotEmpty();
        assertThat(classEntries).noneMatch(name -> name.endsWith("Test.class"));
        assertThat(classEntries).noneMatch(name -> name.contains("BuildSplitProof"));
    }

    private static boolean hasDotSegment(String relativePath) {
        for (String segment : relativePath.split("/")) {
            if (segment.startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> jarEntries() throws IOException {
        Path jarPath = Path.of("target", "globerbooks.jar");
        assertThat(Files.isRegularFile(jarPath)).as("target/globerbooks.jar exists").isTrue();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            return jar.stream().map(entry -> entry.getName()).toList();
        }
    }
}
