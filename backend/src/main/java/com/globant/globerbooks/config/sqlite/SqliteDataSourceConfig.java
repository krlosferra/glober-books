package com.globant.globerbooks.config.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The application's only data source (Task 34a-2). It is built as the guard's check followed by
 * the provider's pool on the vetted location, so the directory (0700), the {@code tmp} directory
 * (0700) and the file (0600) exist with the right modes before the first connection. Because this
 * bean exists, Boot's auto-configured data source backs off. The start policy has already checked
 * the form of {@code spring.datasource.url} when this runs.
 *
 * <p>The location is normalized with {@code toAbsolutePath().normalize()} and never resolved
 * through links: a path that goes through a symbolic link is refused on purpose.
 */
@Configuration(proxyBeanMethods = false)
public class SqliteDataSourceConfig {

    private static final String URL_PROPERTY = "spring.datasource.url";
    private static final String TIMEOUT_PROPERTY = "spring.datasource.hikari.connection-timeout";
    private static final String URL_PREFIX = "jdbc:sqlite:";

    /**
     * Builds the guarded, single-connection data source.
     *
     * @param environment the environment holding the data location and the pool timeout
     * @return the started pool
     * @throws DataDirectoryGuard.Refusal with a fixed reason when the location is refused
     */
    @Bean(destroyMethod = "close")
    public DataSource dataSource(Environment environment) {
        Path file = locate(environment.getProperty(URL_PROPERTY));
        HikariDataSource pool = SqliteDataSourceProvider.create(DataDirectoryGuard.check(file));
        Long timeout = environment.getProperty(TIMEOUT_PROPERTY, Long.class);
        if (timeout != null) {
            pool.setConnectionTimeout(timeout);
        }
        return pool;
    }

    private static Path locate(String url) {
        try {
            if (url == null || !url.startsWith(URL_PREFIX)) {
                throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.LOCATION_NOT_ABSOLUTE);
            }
            return Path.of(url.substring(URL_PREFIX.length())).toAbsolutePath().normalize();
        } catch (InvalidPathException | SecurityException failure) {
            throw new DataDirectoryGuard.Refusal(DataDirectoryGuard.Reason.LOCATION_NOT_ABSOLUTE);
        }
    }
}
