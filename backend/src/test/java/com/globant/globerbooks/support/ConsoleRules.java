package com.globant.globerbooks.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

/** What the console of a refused start must not hold (SC-11, SC-37): paths, book data, stack traces. */
public final class ConsoleRules {

    private ConsoleRules() {
    }

    /**
     * Asserts that the console holds the fixed text, no location path, no marker of book data and no
     * stack trace.
     *
     * @param console the console text
     * @param fixedText the fixed reason that must appear
     * @param forbiddenPaths paths that must not appear (the temporary location and its parts)
     */
    public static void assertFixedAndClean(String console, String fixedText, Path... forbiddenPaths) {
        assertThat(console).contains("APPLICATION FAILED TO START").contains(fixedText);
        assertThat(console).doesNotContain("\tat ").doesNotContain("Caused by")
                .doesNotContain(".java:").doesNotContainPattern("at [\\w.$]+\\(").doesNotContain("ZQXDATA");
        for (Path path : forbiddenPaths) {
            assertThat(console).doesNotContain(path.toString());
        }
    }
}
