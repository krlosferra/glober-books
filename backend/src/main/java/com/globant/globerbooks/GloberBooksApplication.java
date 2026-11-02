package com.globant.globerbooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;

/**
 * Entry point of the GloberBooks backend.
 *
 * <p>The default Spring Boot error handling is excluded because every error body is produced by the
 * application's own error-body factory.
 */
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class GloberBooksApplication {

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GloberBooksApplication.class, args);
    }
}
