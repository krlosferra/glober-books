package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Proves that a class named {@code *Test} is executed by Surefire and not by Failsafe.
 */
class BuildSplitProofTest {

    @Test
    void runsUnderSurefireWhenNamedWithTestSuffix() {
        String runner = System.getProperty("globerbooks.test.runner");

        assertThat(runner).isEqualTo("surefire");
    }
}
