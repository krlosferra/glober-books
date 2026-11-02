package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Checks the committed dependency tree and the classpath of the test run against the constitution's
 * list of banned runtime coordinates.
 *
 * <p>The committed tree is generated with
 * {@code ./mvnw dependency:tree -DoutputFile=config/dependency-tree.txt} from the {@code backend}
 * directory.
 */
class RuntimeClasspathBanTest {

    private static final Path TREE_FILE = Path.of("config", "dependency-tree.txt");

    private static final Pattern COORDINATE =
            Pattern.compile("([\\w.\\-]+):([\\w.\\-]+):jar:(?:([\\w.\\-]+):)?([\\w.\\-]+):(compile|runtime|test|provided)");

    /** Banned coordinates as {@code group:artifact}; {@code *} is a wildcard, a trailing {@code .*} in the group means sub-groups. */
    private static final List<String> BANNED = List.of(
            "org.springframework.boot:spring-boot-starter-actuator",
            "org.springframework.boot:spring-boot-devtools",
            "com.h2database:h2",
            "org.springframework.security:*",
            "org.projectlombok:lombok",
            "org.apache.httpcomponents.*:*",
            "com.squareup.okhttp3:*",
            "org.springframework.boot:spring-boot-starter-webflux",
            "io.projectreactor.netty:*",
            "*:jackson-dataformat-xml",
            "org.springframework.boot:spring-boot-starter-websocket",
            "org.apache.logging.log4j:log4j-core");

    @Test
    void committedTreeContainsNoBannedCoordinate() throws IOException {
        List<Coordinate> tree = readCommittedTree();

        assertThat(tree).isNotEmpty();
        assertThat(tree.stream().filter(RuntimeClasspathBanTest::isBanned).toList()).isEmpty();
    }

    @Test
    void committedTreeHoldsEveryApprovedDirectDependency() throws IOException {
        Set<String> ids = new TreeSet<>();
        readCommittedTree().forEach(coordinate -> ids.add(coordinate.group() + ":" + coordinate.artifact()));

        assertThat(ids).contains(
                "org.springframework.boot:spring-boot-starter-webmvc",
                "org.springframework.boot:spring-boot-starter-validation",
                "org.springframework.boot:spring-boot-starter-data-jpa",
                "org.xerial:sqlite-jdbc",
                "org.hibernate.orm:hibernate-community-dialects",
                "org.springframework.boot:spring-boot-starter-webmvc-test",
                "org.springframework.boot:spring-boot-starter-data-jpa-test");
    }

    @Test
    void everyJarOnTheTestClasspathIsInTheCommittedTree() throws IOException {
        Set<String> treeFiles = new TreeSet<>();
        for (Coordinate coordinate : readCommittedTree()) {
            treeFiles.add(coordinate.jarFileName());
        }
        List<String> jarNames = classpathJarNames();

        assertThat(jarNames).isNotEmpty();
        assertThat(jarNames.stream().filter(name -> !treeFiles.contains(name)).toList()).isEmpty();
    }

    @Test
    void detectsBannedCoordinatesInTreeLines() {
        List<String> bannedLines = List.of(
                "+- org.projectlombok:lombok:jar:1.18.30:provided",
                "+- com.h2database:h2:jar:2.2.224:runtime",
                "+- org.springframework.security:spring-security-core:jar:7.0.0:compile",
                "+- org.apache.httpcomponents.client5:httpclient5:jar:5.3:compile",
                "+- org.apache.httpcomponents:httpclient:jar:4.5:compile",
                "+- tools.jackson.dataformat:jackson-dataformat-xml:jar:3.1.7:compile",
                "+- org.apache.logging.log4j:log4j-core:jar:2.22.0:compile");

        for (String line : bannedLines) {
            Optional<Coordinate> parsed = parse(line);
            assertThat(parsed).as(line).isPresent();
            assertThat(isBanned(parsed.get())).as(line).isTrue();
        }
    }

    @Test
    void acceptsApprovedAndExceptionCoordinatesInTreeLines() {
        List<String> allowedLines = List.of(
                "+- org.springframework.boot:spring-boot-starter-webmvc:jar:4.1.2:compile",
                "+- commons-logging:commons-logging:jar:1.3.6:compile",
                "+- org.apache.tomcat.embed:tomcat-embed-websocket:jar:11.0.27:compile");

        for (String line : allowedLines) {
            Optional<Coordinate> parsed = parse(line);
            assertThat(parsed).as(line).isPresent();
            assertThat(isBanned(parsed.get())).as(line).isFalse();
        }
    }

    @Test
    void bannedClassesAreAbsentFromTheTestClasspath() {
        List<String> bannedClasses = List.of(
                "org.h2.Driver",
                "lombok.Generated",
                "org.apache.logging.log4j.core.LoggerContext",
                "okhttp3.OkHttpClient",
                "org.apache.http.HttpClient",
                "org.springframework.security.core.Authentication");

        for (String className : bannedClasses) {
            boolean present = true;
            try {
                Class.forName(className, false, getClass().getClassLoader());
            } catch (ClassNotFoundException | LinkageError absent) {
                present = false;
            }
            assertThat(present).as(className).isFalse();
        }
    }

    private static boolean isBanned(Coordinate coordinate) {
        for (String pattern : BANNED) {
            int split = pattern.indexOf(':');
            String groupPattern = pattern.substring(0, split);
            String artifactPattern = pattern.substring(split + 1);
            if (groupMatches(groupPattern, coordinate.group()) && artifactMatches(artifactPattern, coordinate.artifact())) {
                return true;
            }
        }
        return false;
    }

    private static boolean groupMatches(String pattern, String group) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String base = pattern.substring(0, pattern.length() - 2);
            return group.equals(base) || group.startsWith(base + ".");
        }
        return group.equals(pattern);
    }

    private static boolean artifactMatches(String pattern, String artifact) {
        return "*".equals(pattern) || artifact.equals(pattern);
    }

    private static List<Coordinate> readCommittedTree() throws IOException {
        List<Coordinate> result = new ArrayList<>();
        for (String line : Files.readAllLines(TREE_FILE)) {
            parse(line).ifPresent(result::add);
        }
        return result;
    }

    private static Optional<Coordinate> parse(String line) {
        Matcher matcher = COORDINATE.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new Coordinate(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
    }

    private static List<String> classpathJarNames() {
        String testPath = System.getProperty("surefire.test.class.path");
        String path = testPath != null ? testPath : System.getProperty("java.class.path");
        List<String> names = new ArrayList<>();
        for (String element : path.split(Pattern.quote(System.getProperty("path.separator")))) {
            String fileName = Path.of(element).getFileName().toString();
            if (fileName.endsWith(".jar") && !fileName.startsWith("surefire")) {
                names.add(fileName);
            }
        }
        return names;
    }

    private record Coordinate(String group, String artifact, String classifier, String version) {

        String jarFileName() {
            String suffix = classifier == null ? "" : "-" + classifier;
            return artifact + "-" + version + suffix + ".jar";
        }
    }
}
