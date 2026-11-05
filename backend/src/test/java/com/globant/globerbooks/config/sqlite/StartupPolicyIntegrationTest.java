package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.GloberBooksApplication;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.unit.DataSize;

class StartupPolicyIntegrationTest {

    @TempDir
    Path tempDir;

    private static Stream<Arguments> refusals() {
        return Stream.of(
                Arguments.of("0.0.0.0", "jdbc:sqlite:%s", StartupPolicy.REASON_ADDRESS),
                Arguments.of("127.0.0.1", "jdbc:sqlite::memory:", StartupPolicy.REASON_URL),
                Arguments.of("127.0.0.1", "jdbc:sqlite:%s?journal_mode=WAL", StartupPolicy.REASON_PARAMETERS),
                Arguments.of("127.0.0.1", "jdbc:sqlite:%s?", StartupPolicy.REASON_PARAMETERS),
                Arguments.of("127.0.0.1", "jdbc:sqlite:%s#frag", StartupPolicy.REASON_PARAMETERS),
                Arguments.of("127.0.0.1", "jdbc:h2:mem:test", StartupPolicy.REASON_URL));
    }

    @ParameterizedTest
    @MethodSource("refusals")
    void refusesToStartWithAFixedReason(String address, String urlTemplate, String reason) {
        String url = urlTemplate.formatted(tempDir.resolve("library.db"));

        Throwable failure = startAndCaptureFailure(address, url);

        assertThat(rootCause(failure)).isInstanceOf(IllegalStateException.class).hasMessage(reason);
        assertThat(tempDir.resolve("library.db")).doesNotExist();
    }

    @Test
    void startsWithAnotherDatabasePath() throws IOException {
        Path other = Files.createDirectory(tempDir.resolve("other")).resolve("library.db");

        try (ConfigurableApplicationContext context = start("127.0.0.1", "jdbc:sqlite:" + other)) {
            assertThat(context.isRunning()).isTrue();
            assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:sqlite:" + other);
        }
    }

    @Test
    void effectiveConfigurationMatchesTheMandatoryProperties() {
        try (ConfigurableApplicationContext context = start("127.0.0.1", "jdbc:sqlite:" + tempDir.resolve("a.db"))) {
            ConfigurableEnvironment env = context.getEnvironment();
            Map<String, String> expected = new LinkedHashMap<>();
            expected.put("spring.jpa.hibernate.ddl-auto", "validate");
            expected.put("spring.jpa.open-in-view", "false");
            expected.put("spring.jpa.show-sql", "false");
            expected.put("spring.sql.init.mode", "always");
            expected.put("spring.jackson.property-naming-strategy", "SNAKE_CASE");
            expected.put("spring.jackson.deserialization.fail-on-unknown-properties", "true");
            expected.put("spring.datasource.hikari.maximum-pool-size", "1");
            expected.put("spring.datasource.hikari.connection-timeout", "5000");
            expected.put("server.address", "127.0.0.1");
            expected.put("server.forward-headers-strategy", "none");
            expected.put("server.max-http-request-header-size", "8KB");
            expected.put("server.tomcat.connection-timeout", "5s");
            expected.put("server.tomcat.accesslog.enabled", "false");
            expected.put("server.error.whitelabel.enabled", "false");
            expected.put("spring.mvc.hiddenmethod.filter.enabled", "false");
            expected.put("spring.mvc.formcontent.filter.enabled", "false");
            expected.put("spring.servlet.multipart.enabled", "false");
            expected.put("spring.web.resources.static-locations", "classpath:/static/");
            expected.put("spring.mvc.log-request-details", "false");
            expected.put("server.tomcat.threads.max", "8");
            expected.put("server.tomcat.threads.min-spare", "2");
            expected.put("server.tomcat.max-connections", "128");
            expected.put("server.tomcat.accept-count", "128");
            expected.put("server.tomcat.max-swallow-size", "16KB");
            expected.forEach((key, value) -> {
                assertThat(env.getProperty(key)).as(key).isEqualTo(value);
            });
            assertBoundValues(context);
        }
    }

    @Test
    void definesNoPropertyThatChangesTheStaticCatalogDirectory() {
        try (ConfigurableApplicationContext context = start("127.0.0.1", "jdbc:sqlite:" + tempDir.resolve("b.db"))) {
            ConfigurableEnvironment env = context.getEnvironment();

            assertThat(env.getProperty("spring.web.resources.static-locations")).isEqualTo("classpath:/static/");
            assertThat(env.getProperty("globerbooks.static.root")).isNull();
            assertThat(env.getProperty("spring.web.resources.add-mappings")).isEqualTo("false");
        }
    }

    private static void assertBoundValues(ConfigurableApplicationContext context) {
        ServerProperties server = context.getBean(ServerProperties.class);
        TomcatServerProperties tomcat = context.getBean(TomcatServerProperties.class);
        assertThat(server.getAddress().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(tomcat.getThreads().getMax()).isEqualTo(8);
        assertThat(tomcat.getThreads().getMinSpare()).isEqualTo(2);
        assertThat(tomcat.getMaxConnections()).isEqualTo(128);
        assertThat(tomcat.getAcceptCount()).isEqualTo(128);
        assertThat(tomcat.getConnectionTimeout().toSeconds()).isEqualTo(5);
        assertThat(tomcat.getMaxSwallowSize()).isEqualTo(DataSize.ofKilobytes(16));
        assertThat(server.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8));
        HikariDataSource pool = context.getBean(HikariDataSource.class);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(1);
        assertThat(pool.getConnectionTimeout()).isEqualTo(5000);
    }

    private ConfigurableApplicationContext start(String address, String url) {
        return new SpringApplication(GloberBooksApplication.class).run(args(address, url));
    }

    private Throwable startAndCaptureFailure(String address, String url) {
        try (ConfigurableApplicationContext context = start(address, url)) {
            throw new AssertionError("The application started with " + context.getId());
        } catch (RuntimeException failure) {
            return failure;
        }
    }

    private static String[] args(String address, String url) {
        return new String[] {"--server.port=0", "--server.address=" + address, "--spring.datasource.url=" + url};
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
