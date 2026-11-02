package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

class StartupPolicyTest {

    private static final String GOOD_URL = "jdbc:sqlite:./data/globerbooks.db";

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "127.0.0.2", "::1", "0:0:0:0:0:0:0:1"})
    void acceptsLoopbackLiterals(String address) {
        assertThatCode(() -> StartupPolicy.validate(address, GOOD_URL)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "192.168.1.5", "::", "localhost", "example.com", "", " ", "127.0.0.1%eth0",
        "[::1]", "10.0.0.1"})
    void refusesNonLoopbackOrNonLiteralAddress(String address) {
        assertThatThrownBy(() -> StartupPolicy.validate(address, GOOD_URL))
                .isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_ADDRESS);
    }

    @Test
    void refusesMissingAddress() {
        assertThatThrownBy(() -> StartupPolicy.validate(null, GOOD_URL))
                .isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_ADDRESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:sqlite::memory:", "jdbc:sqlite:", "jdbc:sqlite:   ", "jdbc:h2:mem:test",
        "jdbc:postgresql://localhost/db", "sqlite:./x.db", "jdbc:sqlite:file:x.db", "jdbc:sqlite:FILE:x.db",
        "jdbc:sqlite:resource:x.db", "jdbc:sqlite:./a\u0000b.db", "jdbc:sqlite:./a\nb.db", ""})
    void refusesUrlsThatAreNotAFile(String url) {
        assertThatThrownBy(() -> StartupPolicy.validate("127.0.0.1", url))
                .isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_URL);
    }

    @Test
    void refusesMissingUrl() {
        assertThatThrownBy(() -> StartupPolicy.validate("127.0.0.1", null))
                .isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_URL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:sqlite:./x.db?journal_mode=WAL", "jdbc:sqlite:./x.db?", "jdbc:sqlite:./x.db#frag",
        "jdbc:sqlite:./x#y.db", "jdbc:sqlite::memory:?cache=shared"})
    void refusesParametersAndFragments(String url) {
        assertThatThrownBy(() -> StartupPolicy.validate("127.0.0.1", url))
                .isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_PARAMETERS);
    }

    @Test
    void refusalReasonsContainNeitherUrlNorAddress() {
        String secretUrl = "jdbc:sqlite:/secret/library.db?journal_mode=WAL";

        assertThatThrownBy(() -> StartupPolicy.validate("0.0.0.0", secretUrl))
                .hasMessageNotContaining("0.0.0.0");
        assertThatThrownBy(() -> StartupPolicy.validate("127.0.0.1", secretUrl))
                .hasMessageNotContaining("secret").hasMessageNotContaining("WAL").hasMessageNotContaining("library");
    }

    @Test
    void acceptsAnotherFilePath() {
        assertThatCode(() -> StartupPolicy.validate("127.0.0.1", "jdbc:sqlite:/tmp/other dir/library.db"))
                .doesNotThrowAnyException();
    }

    @Test
    void postProcessorReadsTheEnvironment() {
        MockEnvironment good = new MockEnvironment().withProperty("server.address", "127.0.0.1")
                .withProperty("spring.datasource.url", GOOD_URL);
        MockEnvironment bad = new MockEnvironment().withProperty("server.address", "0.0.0.0")
                .withProperty("spring.datasource.url", GOOD_URL);

        StartupPolicy policy = new StartupPolicy();

        assertThatCode(() -> policy.postProcessEnvironment(good, null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.postProcessEnvironment(bad, null))
                .isInstanceOf(IllegalStateException.class).hasMessage(StartupPolicy.REASON_ADDRESS);
        assertThat(policy.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void mainSourcesOutsideConfigSqliteDoNotUseFileApis() throws IOException {
        Pattern fileApi = Pattern.compile("java\\.nio\\.file\\.Files|FileInputStream|FileChannel");
        Path root = backendDirectory().resolve(Path.of("src", "main", "java"));
        List<String> hits = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(root)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String relative = root.relativize(source).toString().replace('\\', '/');
                String text = Files.readString(source, StandardCharsets.UTF_8);
                if (!relative.contains("/config/sqlite/") && fileApi.matcher(text).find()) {
                    hits.add(relative);
                }
            }
        }

        assertThat(hits).isEmpty();
    }

    private static Path backendDirectory() {
        try {
            Path classes = Path.of(StartupPolicy.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return classes.getParent().getParent();
        } catch (java.net.URISyntaxException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
