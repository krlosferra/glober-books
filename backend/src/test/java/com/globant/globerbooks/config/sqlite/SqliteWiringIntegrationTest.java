package com.globant.globerbooks.config.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globant.globerbooks.GloberBooksApplication;
import com.globant.globerbooks.support.FileFacts;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Task 34a-2, criterion 1: the application's data source is the guarded one, and its pragmas are read back
 * from the application's own data source (in process, with the real configuration class; the jar-level
 * proofs are in {@code JarDataLocationIntegrationTest} and {@code StartupRefusalIntegrationTest}).
 * Expected values: US-009.3, SC-32, SC-13 (journal mode and modes), SEC-F-185 item 5.
 */
class SqliteWiringIntegrationTest {

    @TempDir
    Path temp;

    private Path data;
    private ConfigurableApplicationContext context;

    @BeforeEach
    void startTheApplicationOnAnEmptyLocation() {
        data = temp.resolve("data");
        context = SpringApplication.run(GloberBooksApplication.class, "--server.port=0",
                "--spring.datasource.url=jdbc:sqlite:" + data.resolve("library.db"));
    }

    @AfterEach
    void stop() {
        context.close();
    }

    @Test
    void theOnlyDataSourceIsTheGuardedPoolAndHibernateUsesIt() {
        Map<String, DataSource> sources = context.getBeansOfType(DataSource.class);

        assertThat(sources).hasSize(1);
        String name = sources.keySet().iterator().next();
        assertThat(context.getBeanFactory().getBeanDefinition(name).getFactoryBeanName())
                .isEqualTo("sqliteDataSourceConfig");
        assertThat(sources.get(name)).isInstanceOf(HikariDataSource.class);
        assertThat(context.getBean("&entityManagerFactory", LocalContainerEntityManagerFactoryBean.class)
                .getDataSource()).isSameAs(sources.get(name));
        assertThat(context.getBean(EntityManagerFactory.class).isOpen()).isTrue();
    }

    @Test
    void pragmasReadBackFromTheRunningDataSource() throws SQLException {
        HikariDataSource pool = (HikariDataSource) context.getBean(DataSource.class);

        try (Connection connection = pool.getConnection(); Statement statement = connection.createStatement()) {
            assertThat(pragma(statement, "foreign_keys")).isEqualTo("1");
            assertThat(pragma(statement, "trusted_schema")).isEqualTo("0");
            assertThat(pragma(statement, "busy_timeout")).isEqualTo("5000");
            assertThat(pragma(statement, "journal_mode")).isEqualTo("delete");
            assertThat(pragma(statement, "locking_mode")).isEqualTo("exclusive");
            assertThat(pragma(statement, "synchronous")).isEqualTo("2");
            assertThatThrownBy(() -> statement.execute("SELECT load_extension('/nonexistent/lib')"))
                    .isInstanceOf(SQLException.class);
        }
        assertThat(pool.getMaximumPoolSize()).isEqualTo(1);
        assertThat(pool.getMinimumIdle()).isEqualTo(1);
        assertThat(pool.getConnectionTimeout()).isEqualTo(5000L);
    }

    @Test
    void theLocationWasCreatedPrivateAndAJournalOfAnOpenWriteIsNoWiderThan0600() throws Exception {
        Path file = data.resolve("library.db");
        assertThat(FileFacts.mode(data)).isEqualTo("rwx------");
        assertThat(FileFacts.mode(data.resolve("tmp"))).isEqualTo("rwx------");
        assertThat(FileFacts.mode(file)).isEqualTo("rw-------");
        assertThat(Files.list(data.resolve("tmp")).toList()).as("the extracted native library is there")
                .isNotEmpty().allMatch(FileFacts::isNotWiderThan0600);
        HikariDataSource pool = (HikariDataSource) context.getBean(DataSource.class);

        try (Connection connection = pool.getConnection(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("INSERT INTO books (id, title, author, status, date_added) VALUES "
                    + "('11111111-1111-4111-8111-111111111111', 't', 'a', 'unread', '2026-01-01T00:00:00.000Z')");
            Path journal = data.resolve("library.db-journal");
            assertThat(journal).as("the rollback journal exists while the write is open").exists();
            assertThat(FileFacts.mode(journal)).isEqualTo("rw-------");
            connection.rollback();
        }
        assertThat(FileFacts.regularFilesNextTo(data, file)).allMatch(FileFacts::isNotWiderThan0600);
    }

    @Test
    void noDataSourceIsBuiltAnyOtherWayInProductionCode() throws Exception {
        List<Method> publicCreators = Arrays.stream(SqliteDataSourceProvider.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("create") && Modifier.isPublic(method.getModifiers()))
                .toList();
        assertThat(publicCreators).hasSize(1);
        assertThat(publicCreators.get(0).getParameterTypes()).containsExactly(DataDirectoryGuard.Guarded.class);

        Path root = Path.of("src", "main", "java");
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path source : walk.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                String name = source.getFileName().toString();
                assertThat(text.contains("new HikariDataSource") || text.contains("new SQLiteDataSource")
                        || text.contains("DriverManager") || text.contains("DataSourceBuilder"))
                        .as(name + " builds a data source").isEqualTo(name.equals("SqliteDataSourceProvider.java"));
                assertThat(text.contains("SqliteDataSourceProvider.create("))
                        .as(name + " calls the provider").isEqualTo(name.equals("SqliteDataSourceConfig.java"));
            }
        }
        String config = Files.readString(root.resolve("com/globant/globerbooks/config/sqlite/"
                + "SqliteDataSourceConfig.java"));
        assertThat(config).contains("SqliteDataSourceProvider.create(DataDirectoryGuard.check(");
    }

    private static String pragma(Statement statement, String name) throws SQLException {
        try (ResultSet result = statement.executeQuery("PRAGMA " + name)) {
            return result.getString(1).toLowerCase(java.util.Locale.ROOT);
        }
    }
}
