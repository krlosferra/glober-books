package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.dto.CreateBookRequest;
import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.unit.DataSize;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * One proof per mandatory property of the constitution, read back from the live context or observed in
 * behaviour on a real port (SC-31 partial, SC-37). Each test prints a PROOF line.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class MandatoryPropertiesIntegrationTest {

    @TempDir
    static Path dataDir;

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ServerProperties server;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("props.db"));
    }

    private static void proof(String property, Object observed) {
        System.out.println("PROOF " + property + " => " + observed);
    }

    private boolean hasBean(String typeName) throws ClassNotFoundException {
        return context.getBeanNamesForType(Class.forName(typeName), true, false).length > 0;
    }

    @Test
    void ddlAutoIsValidateInTheLiveEntityManagerFactory() {
        Object observed = entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto");
        proof("spring.jpa.hibernate.ddl-auto", observed);
        assertThat(observed).isEqualTo("validate");
    }

    @Test
    void openInViewIsOff() throws ClassNotFoundException {
        boolean present = hasBean("org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor");
        proof("spring.jpa.open-in-view", "interceptor bean present=" + present);
        assertThat(present).isFalse();
    }

    @Test
    void showSqlIsOffInTheLiveEntityManagerFactory() {
        Object observed = entityManagerFactory.getProperties().get("hibernate.show_sql");
        boolean bound = context.getBean(JpaProperties.class).isShowSql();
        proof("spring.jpa.show-sql", "JpaProperties=" + bound + ", hibernate.show_sql=" + observed);
        assertThat(bound).isFalse();
        assertThat(observed == null || "false".equals(String.valueOf(observed))).isTrue();
    }

    @Test
    void sqlInitModeAlwaysCreatesTheTableInAnEmptyDatabase() {
        Integer tables = jdbc.queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'books'", Integer.class);
        proof("spring.sql.init.mode", "books table created from schema.sql in a new file, count=" + tables);
        assertThat(tables).isEqualTo(1);
    }

    @Test
    void jacksonWritesSnakeCaseAndRefusesUnknownFields() throws IOException {
        record Probe(String dateAdded) {
        }
        String written = mapper.writeValueAsString(new Probe("x"));
        proof("spring.jackson.property-naming-strategy", written);
        assertThat(written).isEqualTo("{\"date_added\":\"x\"}");
        proof("spring.jackson.deserialization.fail-on-unknown-properties",
                mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
        assertThatThrownBy(() -> mapper.readValue("{\"title\":\"a\",\"author\":\"b\",\"extra\":1}",
                CreateBookRequest.class)).isInstanceOf(JacksonException.class);
        byte[] extra = "{\"title\":\"a\",\"author\":\"b\",\"extra\":1}".getBytes(StandardCharsets.UTF_8);
        Answer unknown = new RawHttpClient(port).request("POST", "/api/v1/books", extra,
                "Content-Type: application/json", "Content-Length: " + extra.length);
        proof("unknown field over HTTP", unknown.status() + " " + unknown.body().replace("\r\n", " "));
        assertThat(unknown.status()).isEqualTo(400);
        assertThat(unknown.body()).contains("\"code\":\"MALFORMED_REQUEST\"");
    }

    @Test
    void poolHasOneConnectionAndFiveSecondTimeout() {
        HikariDataSource pool = (HikariDataSource) dataSource;
        proof("spring.datasource.hikari.maximum-pool-size", pool.getMaximumPoolSize());
        proof("spring.datasource.hikari.connection-timeout", pool.getConnectionTimeout());
        assertThat(pool.getMaximumPoolSize()).isEqualTo(1);
        assertThat(pool.getConnectionTimeout()).isEqualTo(5000L);
    }

    @Test
    void serverListensOnLoopbackOnly() {
        proof("server.address", server.getAddress());
        assertThat(server.getAddress()).isEqualTo(InetAddress.getLoopbackAddress());
    }

    @Test
    void forwardedHeadersAreNotHonoured() throws IOException {
        proof("server.forward-headers-strategy", server.getForwardHeadersStrategy());
        assertThat(server.getForwardHeadersStrategy()).isEqualTo(ServerProperties.ForwardHeadersStrategy.NONE);
        Answer answer = new RawHttpClient(port).request("GET", "/api/v1/books", null,
                "X-Forwarded-Host: evil.example", "X-Forwarded-Proto: https", "Forwarded: host=evil.example");
        proof("forwarded headers over HTTP", answer.status());
        assertThat(answer.status()).isEqualTo(200);
    }

    @Test
    void headerSizeLimitIsEightKibAndEnforced() throws IOException {
        proof("server.max-http-request-header-size", server.getMaxHttpRequestHeaderSize());
        assertThat(server.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8));
        Answer answer = new RawHttpClient(port).request("GET", "/api/v1/books", null, "X-Big: " + "a".repeat(9000));
        proof("9000-byte header over HTTP", answer.status());
        assertThat(answer.status()).isIn(400, 431);
    }

    @Test
    void tomcatConnectionTimeoutIsFiveSecondsAndAccessLogIsOff() {
        TomcatServerProperties tomcat = context.getBean(TomcatServerProperties.class);
        proof("server.tomcat.connection-timeout", tomcat.getConnectionTimeout());
        proof("server.tomcat.accesslog.enabled", tomcat.getAccesslog().isEnabled());
        assertThat(tomcat.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(tomcat.getAccesslog().isEnabled()).isFalse();
    }

    @Test
    void whitelabelIsOffAndTheErrorControllerDoesNotExist() throws IOException {
        boolean controller = context.containsBean("basicErrorController");
        proof("server.error.whitelabel.enabled and ErrorMvcAutoConfiguration excluded",
                "basicErrorController present=" + controller);
        assertThat(controller).isFalse();
        assertThat(environment.getProperty("server.error.whitelabel.enabled", Boolean.class)).isFalse();
        Answer error = new RawHttpClient(port).request("GET", "/error", null);
        proof("GET /error", error.status() + " " + error.body().replace("\r\n", " "));
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.body()).contains("\"code\":\"NOT_FOUND\"").doesNotContain("Whitelabel");
    }

    @Test
    void hiddenMethodFormContentAndMultipartFiltersAreAbsent() throws ClassNotFoundException {
        boolean hidden = hasBean("org.springframework.web.filter.HiddenHttpMethodFilter");
        boolean form = hasBean("org.springframework.web.filter.FormContentFilter");
        boolean multipart = hasBean("org.springframework.web.multipart.MultipartResolver");
        proof("spring.mvc.hiddenmethod / formcontent / spring.servlet.multipart",
                "hidden=" + hidden + " form=" + form + " multipart=" + multipart);
        assertThat(hidden).isFalse();
        assertThat(form).isFalse();
        assertThat(multipart).isFalse();
    }

    @Test
    void staticLocationsAndRequestDetailLoggingAreAsConfigured() {
        WebProperties web = context.getBean(WebProperties.class);
        WebMvcProperties mvc = context.getBean(WebMvcProperties.class);
        proof("spring.web.resources.static-locations", String.join(",", web.getResources().getStaticLocations()));
        proof("spring.mvc.log-request-details", mvc.isLogRequestDetails());
        assertThat(web.getResources().getStaticLocations()).containsExactly("classpath:/static/");
        assertThat(mvc.isLogRequestDetails()).isFalse();
    }

    @Test
    void pinnedLogLevelsAreInfo() {
        for (String name : new String[] {"ROOT", "org.hibernate", "org.springframework.web", "org.apache.tomcat"}) {
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
            proof("logging.level " + name, logger.getEffectiveLevel());
            assertThat(logger.getEffectiveLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        }
    }

    @Test
    void logPatternNeutralizesLineBreaksSeparatorsAndEscapeInMessagesAndExceptions(CapturedOutput output)
            throws IOException {
        Logger log = LoggerFactory.getLogger("proof.sanitisation");
        log.warn("MSG-A first\r\nFORGED-LINE two three\u0085four\u001Bfive end");
        log.error("MSG-B", new IllegalStateException("cause one\nFORGED-CAUSE\u001Bend"));
        new RawHttpClient(port).request("GET", "/x%0d%0aFORGED%e2%80%a8y\u001B", null);

        String all = output.getAll();
        String lineA = lineWith(all, "MSG-A");
        String lineB = lineWith(all, "MSG-B");
        proof("logging.pattern.console message", lineA.replace("\u001B", "<ESC>"));
        proof("logging.pattern.console exception", lineB.substring(0, Math.min(lineB.length(), 160)));
        assertThat(lineA).contains("MSG-A first__FORGED-LINE_two_three_four_five end");
        assertThat(lineB).contains("MSG-B").contains("IllegalStateException: cause one_FORGED-CAUSE_end");
        assertThat(all.lines().filter(line -> line.startsWith("FORGED") || line.startsWith("\tat"))).isEmpty();
        for (String forbidden : new String[] {" ", " ", "\u0085", "\u001B"}) {
            assertThat(all.lines().filter(line -> !line.startsWith("PROOF") && !line.startsWith("EVIDENCE"))
                    .filter(line -> line.contains(forbidden))).as("U+%04X", (int) forbidden.charAt(0)).isEmpty();
        }
    }

    private static String lineWith(String all, String marker) {
        return all.lines().filter(line -> line.contains(marker) && !line.startsWith("PROOF")).findFirst()
                .orElseThrow(() -> new AssertionError("no log line with " + marker));
    }
}
