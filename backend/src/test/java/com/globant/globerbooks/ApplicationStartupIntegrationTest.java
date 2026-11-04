package com.globant.globerbooks;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Path;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * SEC-F-118: the application starts with the shipped {@code application.yml} only (the database
 * file and a free port are the sole overrides), uses the SQLite dialect and passes
 * {@code ddl-auto: validate} against {@code schema.sql}.
 */
class ApplicationStartupIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void startsWithShippedConfigurationUsingTheSqliteDialectAndValidate() {
        String[] args = {"--spring.datasource.url=jdbc:sqlite:" + tempDir.resolve("startup.db"),
            "--server.port=0"};

        try (ConfigurableApplicationContext context = SpringApplication.run(GloberBooksApplication.class, args)) {
            SessionFactoryImplementor factory = context.getBean(EntityManagerFactory.class)
                    .unwrap(SessionFactoryImplementor.class);
            assertThat(context.isRunning()).isTrue();
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(context.getEnvironment().getProperty("spring.jpa.database-platform"))
                    .isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
            assertThat(factory.getJdbcServices().getDialect()).isInstanceOf(SQLiteDialect.class);
        }
    }
}
