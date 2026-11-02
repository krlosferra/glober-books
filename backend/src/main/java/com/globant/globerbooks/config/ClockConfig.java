package com.globant.globerbooks.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the single {@link Clock} of the application. It is the only place in {@code src/main}
 * that reads the wall clock; every other class receives this bean by constructor injection.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /**
     * Creates the application clock.
     *
     * @return a clock in UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
