package com.globant.globerbooks.config.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.globant.globerbooks.config.transport.RouteTable.Decision;
import com.globant.globerbooks.config.transport.RouteTable.Outcome;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Route and method decisions of spec 5.0 and Appendix D, and the guard that ties the table to the controllers. */
class RouteTableTest {

    private static final String CONTROLLERS = "com.globant.globerbooks.controller";
    private static final String BOOK_ID = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";

    @TempDir
    Path staticDir;

    private StaticAssetCatalog catalog;
    private RouteTable table;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(staticDir.resolve("index.html"), "<html></html>");
        Files.writeString(staticDir.resolve("app.js"), "1");
        Files.createDirectory(staticDir.resolve("css"));
        Files.writeString(staticDir.resolve("css/styles.css"), "a{}");
        catalog = new StaticAssetCatalog(staticDir.toUri().toString());
        table = new RouteTable(catalog);
    }

    private Decision decide(String path, String method) {
        return table.decide(RequestTarget.of(path), method);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/unknown", "/api/v1/books/", "/api/v1/books/" + BOOK_ID + "/",
        "/api/v1/books/" + BOOK_ID + ";x=1", "/api/v1/books;x=1", "/api/v1/books.json", "/api/v1/books/x",
        "/api/v1/%62ooks", "/api/v1/%42ooks", "/API/V1/BOOKS", "/api/v1/books/%2e%2e", "/api/v1/books/%2E%2e",
        "/api/v1/books/..", "/api/v1/./books", "//api/v1/books", "/api/v1//books", "/api/v1/books%2f",
        "/api/v1/books\\", "/index.html/", "/index.htm", "/css", "/css/", "/%69ndex.html", "/INDEX.HTML",
        "/missing.js", "", "*"})
    void answersNotFoundForAnyPathThatIsNotInTheTable(String path) {
        assertThat(decide(path, "GET")).isEqualTo(new Decision(Outcome.NOT_FOUND, null));
        assertThat(decide(path, "POST").outcome()).isEqualTo(Outcome.NOT_FOUND);
    }

    @Test
    void answersNotFoundBeforeAnyIdRuleOnPathsThatDoNotExistYet() {
        assertThat(decide("/api/v1/books/not-a-uuid/other", "GET").outcome()).isEqualTo(Outcome.NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void findsTheBookCollectionMethods(String method) {
        assertThat(decide("/api/v1/books", method).outcome()).isEqualTo(Outcome.FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HEAD", "OPTIONS", "PUT", "DELETE", "PATCH", "TRACE", "CONNECT", "get", "post", "FOO"})
    void answersMethodNotAllowedWithGetAndPostOnTheBookCollection(String method) {
        assertThat(decide("/api/v1/books", method)).isEqualTo(new Decision(Outcome.METHOD_NOT_ALLOWED, "GET, POST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/app.js", "/css/styles.css"})
    void findsGetOnThePageAndEveryStaticPath(String path) {
        assertThat(decide(path, "GET").outcome()).isEqualTo(Outcome.FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/app.js", "/css/styles.css"})
    void answersMethodNotAllowedWithGetOnlyForEveryOtherMethodOnStaticPaths(String path) {
        for (String method : new String[] {"HEAD", "OPTIONS", "POST", "PUT", "DELETE", "PATCH"}) {
            assertThat(decide(path, method)).isEqualTo(new Decision(Outcome.METHOD_NOT_ALLOWED, "GET"));
        }
    }

    @Test
    void neverListsHeadOrOptionsInAnyAllowValue() {
        for (String path : table.routes().keySet()) {
            String allow = table.allowFor(RequestTarget.of(path)).orElseThrow();
            assertThat(allow).doesNotContain("HEAD").doesNotContain("OPTIONS");
        }
    }

    @Test
    void givesNoAllowForUndefinedOrNotRoutablePaths() {
        assertThat(table.allowFor(RequestTarget.of("/nope"))).isEmpty();
        assertThat(table.allowFor(RequestTarget.of("/api/v1/books/"))).isEmpty();
        assertThat(table.allowFor(RequestTarget.of("/a/%2e%2e"))).isEmpty();
    }

    /** X-11: the table must equal the mappings of the controllers, so a new endpoint cannot skip it. */
    @Test
    void equalsTheMappingsOfTheControllersAndTheCatalog() {
        Map<String, Set<String>> declared = new TreeMap<>();
        boolean catchAll = false;
        for (Class<?> controller : controllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                assertThat(mapping.path()).as(method.toString()).hasSizeLessThanOrEqualTo(1);
                String path = prefix(controller) + (mapping.path().length == 0 ? "" : mapping.path()[0]);
                Set<String> verbs = new TreeSet<>();
                for (RequestMethod verb : mapping.method()) {
                    verbs.add(verb.name());
                }
                if ("/**".equals(path)) {
                    assertThat(verbs).as("catch-all verbs").containsExactly("GET");
                    catchAll = true;
                } else {
                    declared.merge(path.isEmpty() ? "/" : path, verbs, (left, right) -> {
                        left.addAll(right);
                        return left;
                    });
                }
            }
        }
        assertThat(catchAll).as("the static catch-all mapping exists").isTrue();
        for (String path : catalog.paths()) {
            declared.put(path, new TreeSet<>(Set.of("GET")));
        }
        declared.putIfAbsent("/", new TreeSet<>(Set.of("GET")));

        assertThat(table.routes()).isEqualTo(declared);
    }

    private static String prefix(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        return mapping == null || mapping.path().length == 0 ? "" : mapping.path()[0];
    }

    private static Set<Class<?>> controllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        Set<Class<?>> found = new TreeSet<>((left, right) -> left.getName().compareTo(right.getName()));
        for (BeanDefinition definition : scanner.findCandidateComponents(CONTROLLERS)) {
            try {
                found.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException missing) {
                throw new IllegalStateException(missing);
            }
        }
        assertThat(found).as("controllers found").isNotEmpty();
        return found;
    }
}
