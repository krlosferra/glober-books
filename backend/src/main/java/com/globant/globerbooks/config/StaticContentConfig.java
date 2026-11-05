package com.globant.globerbooks.config;

import com.globant.globerbooks.config.transport.StaticAssetCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Production wiring of the static catalog: it reads {@code classpath:/static/} and nothing else. */
@Configuration(proxyBeanMethods = false)
public class StaticContentConfig {

    /**
     * Builds the catalog once at start.
     *
     * @return the catalog of the packaged frontend
     */
    @Bean
    public StaticAssetCatalog staticAssetCatalog() {
        return new StaticAssetCatalog("classpath:/static/");
    }
}
