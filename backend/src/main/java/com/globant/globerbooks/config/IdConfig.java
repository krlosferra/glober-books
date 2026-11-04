package com.globant.globerbooks.config;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the source of book ids so tests can replace it. */
@Configuration(proxyBeanMethods = false)
public class IdConfig {

    /**
     * Creates the id source.
     *
     * @return a supplier of random version 4 UUIDs
     */
    @Bean
    public Supplier<UUID> bookIdSupplier() {
        return UUID::randomUUID;
    }
}
