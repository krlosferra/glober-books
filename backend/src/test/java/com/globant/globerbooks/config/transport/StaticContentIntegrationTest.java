package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.config.StaticContentConfig;
import com.globant.globerbooks.support.RawHttpClient;
import com.globant.globerbooks.support.RawHttpClient.Answer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Real-port proof of static content (spec 5.8) over a fixture directory built without any property. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StaticContentIntegrationTest.Fixture.class)
class StaticContentIntegrationTest {

    private static final String PAGE = "<!doctype html><title>fixture</title>";

    @TempDir
    static Path dataDir;

    @TempDir
    static Path staticDir;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void writeFixture() throws IOException {
        Files.createDirectories(staticDir.resolve("css"));
        Files.createDirectories(staticDir.resolve("js"));
        Files.writeString(staticDir.resolve("index.html"), PAGE);
        Files.writeString(staticDir.resolve("css/app.css"), "body{margin:0}");
        Files.writeString(staticDir.resolve("js/main.js"), "export const x = 1;");
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("static.db"));
    }

    /** Replaces the production catalog with one built directly from the fixture directory. */
    @TestConfiguration
    static class Fixture {
        @Bean
        @Primary
        StaticAssetCatalog fixtureCatalog() {
            return new StaticAssetCatalog(staticDir.toUri().toString());
        }
    }

    private Answer get(String path, String... headers) throws IOException {
        return new RawHttpClient(port).request("GET", path, null, headers);
    }

    @Test
    void rootAndIndexReturnTheSameBytesWithHtmlType() throws IOException {
        Answer root = get("/");
        Answer index = get("/index.html");

        assertThat(root.status()).isEqualTo(200);
        assertThat(index.status()).isEqualTo(200);
        assertThat(root.body()).isEqualTo(PAGE).isEqualTo(index.body());
        assertThat(root.header("Content-Type")).isEqualTo("text/html;charset=UTF-8");
        assertThat(index.header("Content-Type")).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void cssAndJsHaveTheirContentTypes() throws IOException {
        assertThat(get("/css/app.css").header("Content-Type")).isEqualTo("text/css;charset=UTF-8");
        assertThat(get("/js/main.js").header("Content-Type")).isEqualTo("text/javascript;charset=UTF-8");
        assertThat(get("/js/main.js").body()).isEqualTo("export const x = 1;");
    }

    @Test
    void staticResponsesCarryTheSecurityHeaders() throws IOException {
        Answer answer = get("/index.html");

        assertThat(HeaderPolicy.SECURITY_HEADERS).isNotEmpty();
        for (Map.Entry<String, String> header : HeaderPolicy.SECURITY_HEADERS.entrySet()) {
            assertThat(answer.header(header.getKey())).as(header.getKey()).isEqualTo(header.getValue());
        }
    }

    @Test
    void staticResponsesHaveNoValidatorsOrRanges() throws IOException {
        Answer answer = get("/index.html");

        assertThat(answer.header("ETag")).isNull();
        assertThat(answer.header("Last-Modified")).isNull();
        assertThat(answer.header("Accept-Ranges")).isNull();
    }

    @Test
    void rangeAndConditionalHeadersAreIgnored() throws IOException {
        Answer answer = get("/index.html", "Range: bytes=0-1", "If-None-Match: \"abc\"",
                "If-Modified-Since: Wed, 21 Oct 2099 07:28:00 GMT");

        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body()).isEqualTo(PAGE);
        assertThat(answer.header("Content-Range")).isNull();
        assertThat(answer.header("Accept-Ranges")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/js/", "/css/", "/js", "/favicon.ico", "/js/main.test.js", "/error", "/.git/config",
        "/data/globerbooks.db", "/index.html/", "//index.html", "/js//main.js", "/js/%2e%2e/index.html",
        "/js/../index.html", "/INDEX.HTML", "/api/v1/unknown", "/missing.js", "/.env",
        "/actuator/health", "/index.html;jsessionid=1"})
    void everythingOutsideTheCatalogIsNotFoundInTheEnvelope(String path) throws IOException {
        Answer answer = get(path);

        assertThat(answer.status()).as(path).isEqualTo(404);
        assertThat(answer.header("Content-Type")).startsWith("application/json");
        assertThat(answer.body()).contains("\"code\":\"NOT_FOUND\"");
        assertThat(answer.header("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/%2e%2e/index.html", "/%2e%2e/%2e%2e/etc/passwd", "/%2e%2e%2findex.html",
        "/..%2f..%2fetc/passwd", "/..;/index.html", "/index.html%00"})
    void preApplicationFormsAreNotFoundOrAnEchoFreeBadRequest(String path) throws IOException {
        Answer answer = get(path);

        assertThat(answer.status()).as(path).isIn(400, 404);
        assertThat(answer.body()).doesNotContain("index.html").doesNotContain("passwd");
        assertThat(answer.raw().toLowerCase(Locale.ROOT)).doesNotContain("tomcat").doesNotContain("server:");
    }

    @Test
    void queryOnAnAssetOrRootIsMalformed() throws IOException {
        for (String path : List.of("/?a=1", "/index.html?x", "/css/app.css?v=2", "/js/main.js?%00")) {
            Answer answer = get(path);

            assertThat(answer.status()).as(path).isEqualTo(400);
            assertThat(answer.body()).as(path).contains("\"code\":\"MALFORMED_REQUEST\"");
        }
    }

    @Test
    void anEmptyQueryIsAccepted() throws IOException {
        assertThat(get("/index.html?").status()).isEqualTo(200);
        assertThat(get("/?").status()).isEqualTo(200);
    }

    @Test
    void queryOnAnUnknownPathIsStillNotFound() throws IOException {
        assertThat(get("/missing.js?a=1").status()).isEqualTo(404);
    }

    @Test
    void bodyByteOnAStaticGetIsMalformed() throws IOException {
        Answer answer = new RawHttpClient(port).request("GET", "/index.html",
                "x".getBytes(StandardCharsets.UTF_8), "Content-Length: 1");

        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.body()).contains("\"code\":\"MALFORMED_REQUEST\"");
    }

    @Test
    void nonGetMethodsWithAContentTypeAreRefusedWith405() throws IOException {
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            Answer answer = new RawHttpClient(port).request(method, "/index.html", null, "Content-Length: 0",
                    "Content-Type: application/json");

            assertThat(answer.status()).as(method).isEqualTo(405);
            assertThat(answer.body()).as(method).contains("\"code\":\"METHOD_NOT_ALLOWED\"");
        }
    }

    @Test
    void theCatalogListsOnlyFixtureFiles() {
        assertThat(new StaticAssetCatalog(staticDir.toUri().toString()).paths())
                .containsExactlyInAnyOrder("/css/app.css", "/index.html", "/js/main.js");
    }

    @ParameterizedTest
    @ValueSource(strings = {"notes.txt", "data.json", "logo.png", "app.js.map", "x.test.js", "js/deep.test.js",
        ".hidden", "js/.secret.js", "noextension", "Upper.JS", "page.htm"})
    void anUnsupportedFileStopsTheStartWithAFixedReason(String name, @TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve(name).getParent());
        Files.writeString(root.resolve("index.html"), PAGE);
        Files.writeString(root.resolve(name), "x");

        assertThatThrownBy(() -> new StaticAssetCatalog(root.toUri().toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(StaticAssetCatalog.REJECTED_FILE)
                .satisfies(failure -> assertThat(failure.getMessage()).doesNotContain(name).doesNotContain("/"));
    }

    @Test
    void productionWiringReadsOnlyTheClasspathStaticLocation() {
        List<String> baseline = new StaticContentConfig().staticAssetCatalog().paths();
        String previous = System.getProperty("spring.web.resources.static-locations");
        System.setProperty("spring.web.resources.static-locations", staticDir.toUri().toString());
        System.setProperty("globerbooks.static.root", staticDir.toString());
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("plausible", Map.of(
                    "spring.web.resources.static-locations", staticDir.toUri().toString(),
                    "globerbooks.static.root", staticDir.toString(),
                    "GLOBERBOOKS_STATIC_ROOT", staticDir.toString())));
            context.register(StaticContentConfig.class);
            context.refresh();

            assertThat(context.getBean(StaticAssetCatalog.class).paths()).isEqualTo(baseline)
                    .doesNotContain("/css/app.css");
            assertThat(baseline).contains("/index.html");
        } finally {
            System.clearProperty("globerbooks.static.root");
            if (previous == null) {
                System.clearProperty("spring.web.resources.static-locations");
            } else {
                System.setProperty("spring.web.resources.static-locations", previous);
            }
        }
    }
}
