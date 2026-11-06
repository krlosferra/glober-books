package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Reason;
import com.globant.globerbooks.config.sqlite.DataDirectoryGuard.Refusal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.mock.env.MockEnvironment;

/** Task 34a-2: the location handling of the configuration class and the fixed texts of the analyzer. */
class SqliteDataSourceConfigTest {

    @TempDir
    Path temp;

    private final SqliteDataSourceConfig config = new SqliteDataSourceConfig();

    @Test
    void nulByteInTheLocationBecomesAFixedRefusal() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:sqlite:" + temp + "/da\u0000ta/library.db");

        assertThatThrownBy(() -> config.dataSource(environment)).isInstanceOf(Refusal.class)
                .extracting(failure -> ((Refusal) failure).reason()).isEqualTo(Reason.LOCATION_NOT_ABSOLUTE);
        assertThat(temp.toFile().list()).isEmpty();
    }

    @Test
    void missingOrForeignUrlIsRefused() {
        assertThatThrownBy(() -> config.dataSource(new MockEnvironment())).isInstanceOf(Refusal.class);
        assertThatThrownBy(() -> config.dataSource(new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:x"))).isInstanceOf(Refusal.class);
    }

    @Test
    void pathThroughALinkIsRefused() throws Exception {
        Path real = Files.createDirectory(temp.resolve("real"));
        Path link = Files.createSymbolicLink(temp.resolve("link"), real);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:sqlite:" + link.resolve("data/library.db"));

        assertThatThrownBy(() -> config.dataSource(environment)).isInstanceOf(Refusal.class)
                .extracting(failure -> ((Refusal) failure).reason()).isEqualTo(Reason.SYMBOLIC_LINK);
        assertThat(real.toFile().list()).isEmpty();
    }

    @Test
    void theAnalyzerGivesTheFixedTextOfAGuardRefusalFoundInACauseChain() {
        Throwable wrapped = new RuntimeException("outer /secret/path",
                new Refusal(Reason.PERMISSIONS_TOO_WIDE));

        FailureAnalysis analysis = new StartupFailureAnalyzer().analyze(wrapped);

        assertThat(analysis.getDescription()).isEqualTo(Reason.PERMISSIONS_TOO_WIDE.text() + ".");
        assertThat(analysis.getAction()).isEqualTo(StartupFailureAnalyzer.ACTION);
        assertThat(analysis.getCause()).isNull();
    }

    @Test
    void theAnalyzerGivesTheFixedTextOfEveryStartPolicyReason() {
        for (String reason : new String[] {StartupPolicy.REASON_ADDRESS, StartupPolicy.REASON_URL,
            StartupPolicy.REASON_PARAMETERS}) {
            FailureAnalysis analysis = new StartupFailureAnalyzer().analyze(new IllegalStateException(reason));
            assertThat(analysis.getDescription()).isEqualTo(reason);
        }
    }

    @Test
    void theAnalyzerLeavesOtherFailuresAlone() {
        assertThat(new StartupFailureAnalyzer().analyze(new IllegalStateException("something else with /path")))
                .isNull();
        assertThat(new StartupFailureAnalyzer().analyze(new RuntimeException())).isNull();
    }
}
